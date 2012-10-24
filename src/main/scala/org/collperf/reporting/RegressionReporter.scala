package org.collperf
package reporting



import java.util.Date
import collection._
import utils.Tree
import utils.Statistics._



case class RegressionReporter(test: RegressionReporter.Tester) extends Reporter {
  import RegressionReporter.ansi

  def report(results: Tree[CurveData], persistor: Persistor) {
    val oks = for {
      (context, curves) <- results.scopes
      if curves.nonEmpty
      _ = log(s"${ansi.green}Test group: ${context.scope}${ansi.reset}")
      history = persistor.load(context)
      curvetable = history.results.map(_._3).flatten.groupBy(_.context.curve)
      curvedata <- curves
    } yield {
      val corresponding = curvetable.getOrElse(curvedata.context.curve, Seq(curvedata))
      val passed = test(context, curvedata, corresponding)
      if (passed) persistor.save(context, curves)
      passed
    }

    val failure = oks.count(_ == false)
    val success = oks.count(_ == true)
    val color = if (failure == 0) ansi.green else ansi.red
    log(s"${color} Summary: $success tests passed, $failure tests failed.")
  }

}


object RegressionReporter {

  object ansi {
    val red = "\u001B[31m"
    val green = "\u001B[32m"
    val yellow = "\u001B[33m"
    val reset = "\u001B[0m"
  }

  trait Tester {
    def apply(context: Context, curvedata: CurveData, corresponding: Seq[CurveData]): Boolean
  }

  object Tester {

    case class ANOVA(defaultSignificance: Double) extends Tester {
      def apply(context: Context, curvedata: CurveData, corresponding: Seq[CurveData]): Boolean = {
        log(s"${ansi.green}- ${curvedata.context.curve} measurements:${ansi.reset}")

        val significance = curvedata.context.goe(Key.significance, defaultSignificance)
        val allmeasurements = (corresponding :+ curvedata) map (_.measurements)
        val measurementtable = allmeasurements.flatten.groupBy(_.params)
        val pointspassed = for {
          measurement <- curvedata.measurements.sorted
        } yield {
          val alternatives = measurementtable(measurement.params).map(_.complete)
          try {
            val ftest = ANOVAFTest(alternatives, significance)
            val color = if (ftest) ansi.green else ansi.red
            val passed = if (ftest) "passed" else "failed"

            log(s"$color  - curve ${curvedata.context.curve} at ${measurement.params}: $passed")
            log(s"    (SSA: ${ftest.ssa}, SSE: ${ftest.sse}, F: ${ftest.F}, qf: ${ftest.quantile}, significance: $significance)${ansi.reset}")

            ftest.result
          } catch {
            case e: Exception =>
              log("Error in ANOVA F-test: " + e.getMessage)
              false
          }
        }

        pointspassed.forall(_ == true)
      }
    }

    case class ConfidenceIntervals(defaultSignificance: Double) extends Tester {
      def apply(context: Context, curvedata: CurveData, corresponding: Seq[CurveData]): Boolean = {
        log(s"${ansi.green}- ${curvedata.context.curve} measurements:${ansi.reset}")

        // TODO

        false
      }
    }

  }

}















