package org.scalameter
package reporting



import java.util.Date
import collection._
import utils.Tree
import utils.Statistics._



case class RegressionReporter(test: RegressionReporter.Tester, historian: RegressionReporter.Historian) extends Reporter {
  import RegressionReporter.ansi

  private val historyCache = mutable.Map[Context, History]()

  def loadHistory(ctx: Context, persistor: Persistor) = historyCache.get(ctx) match {
    case Some(h) => h
    case None =>
      val h = persistor.load(ctx)
      historyCache.put(ctx, h)
      h
  }

  def report(curvedata: CurveData, persistor: Persistor) {
    val ctx = curvedata.context
    val history = loadHistory(ctx, persistor)
    val corresponding = if (history.curves.nonEmpty) history.curves else Seq(curvedata)
    test(ctx, curvedata, corresponding)
  }

  def report(results: Tree[CurveData], persistor: Persistor) = {
    log("")
    log(s"${ansi.green}:::Summary of regression test results - $test:::${ansi.reset}")

    val currentDate = new Date
    val oks = for {
      (context, curves) <- results.scopes
      if curves.nonEmpty
    } yield {
      log(s"${ansi.green}Test group: ${context.scope}${ansi.reset}")

      val testedcurves = for (curvedata <- curves) yield {
        val history = loadHistory(curvedata.context, persistor)
        val corresponding = if (history.curves.nonEmpty) history.curves else Seq(curvedata)

        val testedcurve = test(context, curvedata, corresponding)

        val newhistory = historian.bookkeep(curvedata.context, history, testedcurve, currentDate)
        persistor.save(curvedata.context, newhistory)

        testedcurve
      }

      log("")

      val allpassed = testedcurves.forall(_.measurements.forall(_.success))
      if (allpassed) events.emit(Event(context.scope, "Test succeeded", Events.Success, null))
      else events.emit(Event(context.scope, "Test failed", Events.Failure, null))

      allpassed
    }

    val failure = oks.count(_ == false)
    val success = oks.count(_ == true)
    val color = if (failure == 0) ansi.green else ansi.red
    log(s"${color} Summary: $success tests passed, $failure tests failed.${ansi.reset}")

    failure == 0
  }

}


object RegressionReporter {

  import Key._

  object ansi {
    val colors = initialContext.goe(Key.reports.colors, true)
    def ifcolor(s: String) = if (colors) s else ""

    val red = ifcolor("\u001B[31m")
    val green = ifcolor("\u001B[32m")
    val yellow = ifcolor("\u001B[33m")
    val reset = ifcolor("\u001B[0m")
  }

  /** Represents a policy for adding the newest result to the history of results.
   */
  trait Historian {
    /** Given an old history and the latest curve and its date, returns a new history,
     *  possibly discarding some of the entries.
     */
    def bookkeep(ctx: Context, h: History, newest: CurveData, d: Date): History
  }

  object Historian {

    /** Preserves all historic results.
     */
    case class Complete() extends Historian {
      def bookkeep(ctx: Context, h: History, newest: CurveData, d: Date) = History(h.results :+ ((d, ctx, newest)))
    }

    /** Preserves only last `size` results.
     */
    case class Window(size: Int) extends Historian {
      def bookkeep(ctx: Context, h: History, newest: CurveData, d: Date) = {
        val newseries = h.results :+ ((d, ctx, newest))
        val prunedhistory = h.copy(results = newseries.takeRight(size))
        prunedhistory
      }
    }

    /** Implements a checkpointing strategy such that the number of preserved results
     *  decreases exponentially with the age of the result.
     */
    case class ExponentialBackoff() extends Historian {

      def push(series: Seq[History.Entry], indices: Seq[Long], newest: History.Entry): History = {
        val entries = series.reverse zip indices
        val sizes = Stream.from(0).map(1L << _).scanLeft(0L)(_ + _)
        val buckets = sizes zip sizes.tail
        val bucketed = buckets map {
          case (from, to) => entries filter {
            case (_, idx) => from < idx && idx <= to
          }
        }
        val pruned = bucketed takeWhile { _.nonEmpty } map { elems =>
          val (last, lastidx) = elems.last
          (last, lastidx + 1)
        }
        val (newentries, newindices) = pruned.unzip

        History(newentries.toBuffer.reverse :+ newest, Map(reports.regression.timeIndices -> (1L +: newindices.toBuffer)))
      }

      def push(h: History, newest: History.Entry): History = {
        log.verbose("Pushing to history with info: " + h.infomap)

        val indices = h.info[Seq[Long]](reports.regression.timeIndices, (0 until h.results.length) map { 1L << _ })
        val newhistory = push(h.results, indices, newest)

        log.verbose("New history info: " + newhistory.infomap)

        newhistory
      }

      def bookkeep(ctx: Context, h: History, newest: CurveData, d: Date) = push(h, (d, ctx, newest))
    }

  }

  /** Performance regression testing mechanism.
   */
  trait Tester {
    /** Given a test performed in a specific `context`, the latest curve (set of measurements) `curvedata`
     *  and previous curves (sets of measurements) for this test `corresponding`, yields a new version
     *  of the latest curve, such that if any of the tests fail, the new sequence of curves will have the
     *  `success` field set to `false` for those measurements that are considered to fail the test.
     */
    def apply(context: Context, curvedata: CurveData, corresponding: Seq[CurveData]): CurveData

    /** Returns a confidence interval for a given set of observations.
     */
    def confidenceInterval(ctx: Context, alt: Seq[Double]): (Double, Double) = sys.error("Confidence intervals can only be computed by testers which use them.")
  }

  object Tester {

    /** Accepts any test result.
     */
    case class Accepter() extends Tester {
      def apply(context: Context, curvedata: CurveData, corresponding: Seq[CurveData]): CurveData = {
        log(s"${ansi.green}- ${context.scope}.${curvedata.context.curve} measurements:${ansi.reset}")

        curvedata
      }
    }

    /** Applies analysis of variance to determine whether some test is statistically different.
     */
    case class ANOVA() extends Tester {
      def apply(context: Context, curvedata: CurveData, corresponding: Seq[CurveData]): CurveData = {
        log(s"${ansi.green}- ${context.scope}.${curvedata.context.curve} measurements:${ansi.reset}")

        val significance = curvedata.context.goe(reports.regression.significance, 1e-10)
        val allmeasurements = (corresponding :+ curvedata) map (_.measurements)
        val measurementtable = allmeasurements.flatten.groupBy(_.params)
        val testedmeasurements = for {
          measurement <- curvedata.measurements.sorted
        } yield {
          val units = measurement.units
          val alternatives = measurementtable(measurement.params).filter(_.success).map(_.complete)
          try {
            val ftest = ANOVAFTest(alternatives, significance)
            val color = if (ftest) ansi.green else ansi.red
            val passed = if (ftest) "passed" else "failed"

            log(s"$color  - at ${measurement.params.axisData.mkString(", ")}, ${alternatives.size} alternatives: $passed${ansi.reset}")
            log(f"$color    (SSA: ${ftest.ssa}%.2f, SSE: ${ftest.sse}%.2f, F: ${ftest.F}%.2f, qf: ${ftest.quantile}%.2f, significance: $significance)${ansi.reset}")
            if (!ftest) {
              def logalt(a: Seq[Double], units: String) = log(s"$color      ${a.map(_ + units).mkString(", ")}${ansi.reset}")
              log(s"$color    History:")
              for (a <- alternatives.init) logalt(a, units)
              log(s"$color    Latest:")
              logalt(alternatives.last, units)
            }

            if (ftest.passed) measurement else measurement.failed
          } catch {
            case e: Exception =>
              log(s"${ansi.red}    Error in ANOVA F-test: ${e.getMessage}${ansi.reset}")
              measurement.failed
          }
        }
        val newcurvedata = curvedata.copy(measurements = testedmeasurements)

        newcurvedata
      }
    }

    case class ConfidenceIntervals(strict: Boolean = false) extends Tester {
      def cistr(ci: (Double, Double), units: String) = f"<${ci._1}%.2f $units, ${ci._2}%.2f $units>"

      def single(previous: Measurement, latest: Measurement, sig: Double): Measurement = {
        try {
          val citest = ConfidenceIntervalTest(strict, previous.complete, latest.complete, sig)
          val units = latest.units
          
          if (!citest) {
            val color = ansi.red
            val ciprev = cistr(citest.ci1, units)
            val cilate = cistr(citest.ci2, units)
            val prevform = previous.complete.map(v => f"$v%.2f")
            val lateform = latest.complete.map(v => f"$v%.2f")
            log.error(
              f"$color      Failed confidence interval test: <${citest.ci._1}%.2f $units, ${citest.ci._2}%.2f $units> ${ansi.reset}\n" +
              f"$color      Previous (mean = ${citest.m1}%.2f $units, stdev = ${citest.s1}%.2f $units, ci = $ciprev): ${prevform.mkString(", ")}${ansi.reset}\n" +
              f"$color      Latest   (mean = ${citest.m2}%.2f $units, stdev = ${citest.s2}%.2f $units, ci = $cilate): ${lateform.mkString(", ")}${ansi.reset}"
            )
            latest.failed
          } else latest
        } catch {
          case e: Exception =>
            log.error(s"${ansi.red}    Error in confidence interval test: ${e.getMessage}${ansi.reset}")
            latest.failed
        }
      }

      def multiple(context: Context, previouss: Seq[Measurement], latest: Measurement): Measurement = {
        val sig = context.goe(reports.regression.significance, 1e-10)
        val tests = for (previous <- previouss if previous.success) yield single(previous, latest, sig)
        val allpass = tests.forall(_.success)
        val color = if (allpass) ansi.green else ansi.red
        val passed = if (allpass) "passed" else "failed"
        val ci = confidenceInterval(context, latest.complete)
        val cis = cistr(ci, latest.units)
        log(s"$color  - at ${latest.params.axisData.mkString(", ")}, ${previouss.size} alternatives: $passed${ansi.reset}")
        log(s"$color    (ci = $cis, significance = $sig)${ansi.reset}")
        tests.find(!_.success).getOrElse(latest)
      }

      def apply(context: Context, curvedata: CurveData, corresponding: Seq[CurveData]): CurveData = {
        log(s"${ansi.green}- ${context.scope}.${curvedata.context.curve} measurements:${ansi.reset}")

        val previousmeasurements = corresponding map (_.measurements)
        val measurementtable = previousmeasurements.flatten.groupBy(_.params)
        val newmeasurements = for {
          measurement <- curvedata.measurements
        } yield {
          multiple(curvedata.context, measurementtable(measurement.params), measurement)
        }

        curvedata.copy(measurements = newmeasurements)
      }

      override def confidenceInterval(context: Context, alt: Seq[Double]): (Double, Double) = {
        val significance = context.goe(reports.regression.significance, 1e-10)

        val citest = ConfidenceIntervalTest(strict, alt, alt, significance)
        citest.ci1
      }
    }

    case class OverlapIntervals() extends Tester {
      def cistr(ci: (Double, Double), units: String) = f"<${ci._1}%.2f $units, ${ci._2}%.2f $units>"

      def single(previous: Measurement, latest: Measurement, sig: Double, noiseMagnitude: Double): Measurement = {
        try {
          val citest = OverlapTest(previous.complete, latest.complete, sig, noiseMagnitude)
          val units = latest.units
          
          if (!citest) {
            val color = ansi.red
            val ciprev = cistr(citest.ci1, units)
            val cilate = cistr(citest.ci2, units)
            val prevform = previous.complete.map(v => f"$v%.2f")
            val lateform = latest.complete.map(v => f"$v%.2f")
            val msg = {
              f"$color      Failed overlap interval test. ${ansi.reset}\n" +
              f"$color      Previous (mean = ${citest.m1}%.2f $units, stdev = ${citest.s1}%.2f $units, ci = $ciprev): ${prevform.mkString(", ")}${ansi.reset}\n" +
              f"$color      Latest   (mean = ${citest.m2}%.2f $units, stdev = ${citest.s2}%.2f $units, ci = $cilate): ${lateform.mkString(", ")}${ansi.reset}"
            }
            log.error(msg)
            latest.failed
          } else latest
        } catch {
          case e: Exception =>
            log.error(s"${ansi.red}    Error in overlap interval test: ${e.getMessage}${ansi.reset}")
            latest.failed
        }
      }

      def multiple(context: Context, previouss: Seq[Measurement], latest: Measurement): Measurement = {
        val sig = context.goe(reports.regression.significance, 1e-10)
        val noiseMagnitude = context.goe(Key.reports.regression.noiseMagnitude, 0.0)
        val tests = for (previous <- previouss if previous.success) yield single(previous, latest, sig, noiseMagnitude)
        val allpass = tests.forall(_.success)
        val color = if (allpass) ansi.green else ansi.red
        val passed = if (allpass) "passed" else "failed"
        val ci = confidenceInterval(context, latest.complete)
        val cis = cistr(ci, latest.units)
        log(s"$color  - at ${latest.params.axisData.mkString(", ")}, ${previouss.size} alternatives: $passed${ansi.reset}")
        log(s"$color    (ci = $cis, significance = $sig)${ansi.reset}")
        tests.find(!_.success).getOrElse(latest)
      }

      def apply(context: Context, curvedata: CurveData, corresponding: Seq[CurveData]): CurveData = {
        log(s"${ansi.green}- ${context.scope}.${curvedata.context.curve} measurements:${ansi.reset}")

        val previousmeasurements = corresponding map (_.measurements)
        val measurementtable = previousmeasurements.flatten.groupBy(_.params)
        val newmeasurements = for {
          measurement <- curvedata.measurements
        } yield {
          multiple(context, measurementtable(measurement.params), measurement)
        }

        curvedata.copy(measurements = newmeasurements)
      }

      override def confidenceInterval(context: Context, alt: Seq[Double]): (Double, Double) = {
        val significance = context.goe(reports.regression.significance, 1e-10)
        val noisemag = context.goe(Key.reports.regression.noiseMagnitude, 0.0)

        val test = OverlapTest(alt, alt, significance, noisemag)
        test.ci1
      }
    }

  }

}















