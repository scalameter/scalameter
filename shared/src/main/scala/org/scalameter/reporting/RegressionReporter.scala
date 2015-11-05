package org.scalameter
package reporting



import java.util.Date
import scala.collection._
import scala.Numeric.Implicits._
import org.scalameter.utils.Tree
import org.scalameter.utils.Statistics._



case class RegressionReporter[T: Numeric](test: RegressionReporter.Tester,
  historian: RegressionReporter.Historian) extends Reporter[T] {
  import RegressionReporter.ansi

  private val historyCache = mutable.Map[Context, History[T]]()

  def loadHistory(ctx: Context, persistor: Persistor) = historyCache.get(ctx) match {
    case Some(h) => h
    case None =>
      val h = persistor.load[T](ctx)
      historyCache.put(ctx, h)
      h
  }

  def report(curvedata: CurveData[T], persistor: Persistor) {
    val ctx = curvedata.context
    val history = loadHistory(ctx, persistor)
    val corresponding = if (history.curves.nonEmpty) history.curves else Seq(curvedata)
    test(ctx, curvedata, corresponding)
  }

  def report(results: Tree[CurveData[T]], persistor: Persistor) = {
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

      val allpassed = testedcurves.forall(_.success)
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
    val colors = currentContext(Key.reports.colors)
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
    def bookkeep[T](ctx: Context, h: History[T], newest: CurveData[T], d: Date): History[T]
  }

  object Historian {

    /** Preserves all historic results.
     */
    case class Complete() extends Historian {
      def bookkeep[T](ctx: Context, h: History[T], newest: CurveData[T], d: Date) =
        History(h.results :+ ((d, ctx, newest)))
    }

    /** Preserves only last `size` results.
     */
    case class Window(size: Int) extends Historian {
      def bookkeep[T](ctx: Context, h: History[T], newest: CurveData[T], d: Date) = {
        val newseries = h.results :+ ((d, ctx, newest))
        val prunedhistory = h.copy(results = newseries.takeRight(size))
        prunedhistory
      }
    }

    /** Implements a checkpointing strategy such that the number of preserved results
     *  decreases exponentially with the age of the result.
     */
    case class ExponentialBackoff() extends Historian {

      def push[T](series: Seq[History.Entry[T]], indices: Seq[Long],
        newest: History.Entry[T]): History[T] = {
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

        History(newentries.toBuffer.reverse :+ newest, immutable.Map(reports.regression.timeIndices -> (1L +: newindices.toBuffer)))
      }

      def push[T](h: History[T], newest: History.Entry[T]): History[T] = {
        log.verbose("Pushing to history with info: " + h.infomap)

        val indices = h.info[Seq[Long]](reports.regression.timeIndices, (0 until h.results.length) map { 1L << _ })
        val newhistory = push(h.results, indices, newest)

        log.verbose("New history info: " + newhistory.infomap)

        newhistory
      }

      def bookkeep[T](ctx: Context, h: History[T], newest: CurveData[T], d: Date) =
        push(h, (d, ctx, newest))
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
    def apply[T: Numeric](context: Context, curvedata: CurveData[T], corresponding: Seq[CurveData[T]]): CurveData[T]

    /** Returns a confidence interval for a given set of observations.
     */
    def confidenceInterval[T: Numeric](ctx: Context, alt: Seq[T]): (Double, Double) =
      sys.error("Confidence intervals can only be computed by testers which use them.")
  }

  object Tester {

    /** Accepts any test result.
     */
    case class Accepter() extends Tester {
      def cistr(ci: (Double, Double), units: String) = f"<${ci._1}%.2f $units, ${ci._2}%.2f $units>"

      def apply[T: Numeric](context: Context, curvedata: CurveData[T],
        corresponding: Seq[CurveData[T]]): CurveData[T] = {
        log(s"${ansi.green}- ${context.scope}.${curvedata.context.curve} measurements:${ansi.reset}")

        for (measurement <- curvedata.measurements) {
          val color = ansi.green
          val passed = "passed"
          val mean = measurement.complete.sum.toDouble() / measurement.complete.size
          val means = f"$mean%.2f ${measurement.units}"
          val ci = confidenceInterval(context, measurement.complete)
          val cis = cistr(ci, measurement.units)
          val sig = context(reports.regression.significance)
          log(s"$color  - at ${measurement.params.axisData.mkString(", ")}: $passed${ansi.reset}")
          log(s"$color    (mean = $means, ci = $cis, significance = $sig)${ansi.reset}")
        }

        curvedata
      }

      override def confidenceInterval[T: Numeric](context: Context,
        alt: Seq[T]): (Double, Double) = {
        val significance = context(reports.regression.significance)

        val citest = ConfidenceIntervalTest(true, alt.map(_.toDouble()),
          alt.map(_.toDouble()), significance)
        citest.ci1
      }
    }

    /** Applies analysis of variance to determine whether some test is statistically different.
     */
    case class ANOVA() extends Tester {
      def apply[T: Numeric](context: Context, curvedata: CurveData[T],
        corresponding: Seq[CurveData[T]]): CurveData[T] = {
        log(s"${ansi.green}- ${context.scope}.${curvedata.context.curve} measurements:${ansi.reset}")

        val significance = curvedata.context(reports.regression.significance)
        val allmeasurements = (corresponding :+ curvedata) map (_.measurements)
        val measurementtable = allmeasurements.flatten.groupBy(_.params)
        val testedmeasurements = for {
          measurement <- curvedata.measurements.sorted
        } yield {
          val units = measurement.units
          val alternatives = measurementtable(measurement.params).filter(_.success).map(_.complete)
          try {
            val ftest = ANOVAFTest(alternatives.map(_.map(_.toDouble())), significance)
            val color = if (ftest) ansi.green else ansi.red
            val passed = if (ftest) "passed" else "failed"

            log(s"$color  - at ${measurement.params.axisData.mkString(", ")}, ${alternatives.size} alternatives: $passed${ansi.reset}")
            log(f"$color    (SSA: ${ftest.ssa}%.2f, SSE: ${ftest.sse}%.2f, F: ${ftest.F}%.2f, qf: ${ftest.quantile}%.2f, significance: $significance)${ansi.reset}")
            if (!ftest) {
              def logalt(a: Seq[T], units: String) =
                log(s"$color      ${a.map(_.toString + units).mkString(", ")}${ansi.reset}")
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
      import scala.Numeric.Implicits._

      def cistr(ci: (Double, Double), units: String) = f"<${ci._1}%.2f $units, ${ci._2}%.2f $units>"

      def single[T: Numeric](previous: Measurement[T],
        latest: Measurement[T], sig: Double): Measurement[T] = {
        try {
          val citest = ConfidenceIntervalTest(strict, previous.complete.map(_.toDouble()),
            latest.complete.map(_.toDouble()), sig)
          val units = latest.units
          
          if (!citest) {
            val color = ansi.red
            val ciprev = cistr(citest.ci1, units)
            val cilate = cistr(citest.ci2, units)
            val prevform = previous.complete.map(v => f"${v.toDouble()}%.2f")
            val lateform = latest.complete.map(v => f"${v.toDouble()}%.2f")
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

      def multiple[T: Numeric](context: Context, previouss: Seq[Measurement[T]],
        latest: Measurement[T]): Measurement[T] = {
        val sig = context(reports.regression.significance)
        val tests = for (previous <- previouss if previous.success) yield single(previous, latest, sig)
        val allpass = tests.forall(_.success)
        val color = if (allpass) ansi.green else ansi.red
        val passed = if (allpass) "passed" else "failed"
        val ci = confidenceInterval(context, latest.complete.map(_.toDouble()))
        val cis = cistr(ci, latest.units)
        log(s"$color  - at ${latest.params.axisData.mkString(", ")}, ${previouss.size} alternatives: $passed${ansi.reset}")
        log(s"$color    (ci = $cis, significance = $sig)${ansi.reset}")
        tests.find(!_.success).getOrElse(latest)
      }

      def apply[T: Numeric](context: Context, curvedata: CurveData[T],
        corresponding: Seq[CurveData[T]]): CurveData[T] = {
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

      override def confidenceInterval[T: Numeric](context: Context,
        alt: Seq[T]): (Double, Double) = {
        val significance = context(reports.regression.significance)

        val citest = ConfidenceIntervalTest(strict, alt.map(_.toDouble()),
          alt.map(_.toDouble()), significance)
        citest.ci1
      }
    }

    case class OverlapIntervals() extends Tester {
      import scala.Numeric.Implicits._

      def cistr(ci: (Double, Double), units: String) = f"<${ci._1}%.2f $units, ${ci._2}%.2f $units>"

      def single[T: Numeric](previous: Measurement[T], latest: Measurement[T],
        sig: Double, noiseMagnitude: Double): Measurement[T] = {
        try {
          val citest = OverlapTest(previous.complete.map(_.toDouble()),
            latest.complete.map(_.toDouble()), sig, noiseMagnitude)
          val units = latest.units
          
          if (!citest) {
            val color = ansi.red
            val ciprev = cistr(citest.ci1, units)
            val cilate = cistr(citest.ci2, units)
            val prevform = previous.complete.map(v => f"${v.toDouble()}%.2f")
            val lateform = latest.complete.map(v => f"${v.toDouble()}%.2f")
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

      def multiple[T: Numeric](context: Context, previouss: Seq[Measurement[T]],
        latest: Measurement[T]): Measurement[T] = {
        val sig = context(reports.regression.significance)
        val noiseMagnitude = context(Key.reports.regression.noiseMagnitude)
        val tests = for (previous <- previouss if previous.success) yield single(previous, latest, sig, noiseMagnitude)
        val allpass = tests.forall(_.success)
        val color = if (allpass) ansi.green else ansi.red
        val passed = if (allpass) "passed" else "failed"
        val ci = confidenceInterval(context, latest.complete.map(_.toDouble()))
        val cis = cistr(ci, latest.units)
        log(s"$color  - at ${latest.params.axisData.mkString(", ")}, ${previouss.size} alternatives: $passed${ansi.reset}")
        log(s"$color    (ci = $cis, significance = $sig)${ansi.reset}")
        tests.find(!_.success).getOrElse(latest)
      }

      def apply[T: Numeric](context: Context, curvedata: CurveData[T],
        corresponding: Seq[CurveData[T]]): CurveData[T] = {
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

      override def confidenceInterval[T: Numeric](context: Context,
        alt: Seq[T]): (Double, Double) = {
        val significance = context(reports.regression.significance)
        val noisemag = context(Key.reports.regression.noiseMagnitude)

        val test = OverlapTest(alt.map(_.toDouble()), alt.map(_.toDouble()), significance, noisemag)
        test.ci1
      }
    }

  }

}















