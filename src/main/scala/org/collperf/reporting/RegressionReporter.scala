package org.collperf
package reporting



import java.util.Date
import collection._
import utils.Tree
import utils.Statistics._



case class RegressionReporter() extends Reporter {

  object ansi {
    val red = "\u001B[31m"
    val green = "\u001B[32m"
    val yellow = "\u001B[33m"
    val reset = "\u001B[0m"
  }

  def test(context: Context, curves: Seq[CurveData], history: History): Seq[Boolean] = {
    log(s"${ansi.green}Test group: ${context.scope}${ansi.reset}")

    val curvetable = history.results.map(_._3).flatten.groupBy(_.context.curve)

    val curvespassed = for {
      curvedata <- curves
      confidence = curvedata.context.goe(Key.confidence, 0.9)
    } yield {
      val pointspassed = for {
        corresponding <- curvetable.get(curvedata.context.curve).orElse(Some(Seq(curvedata))).toSeq
        allmeasurements = (corresponding :+ curvedata) map (_.measurements)
        measurementtable = allmeasurements.flatten.groupBy(_.params)
        measurement <- curvedata.measurements
      } yield {
        val alternatives = measurementtable(measurement.params).map(_.complete)
        val ssa = SSA(alternatives)
        val sse = SSE(alternatives)
        val ftest = ANOVAFTest(alternatives, 1 - confidence)

        val color = if (ftest) ansi.green else ansi.red
        val passed = if (ftest) "passed" else "failed"
        log(s"$color- curve ${curvedata.context.curve} at ${measurement.params}: $passed")
        log(s"  (SSA: $ssa, SSE: $sse, confidence: $confidence)${ansi.reset}")

        ftest
      }

      pointspassed.forall(_ == true)
    }

    curvespassed
  }

  def report(results: Tree[CurveData], persistor: Persistor) {
    val oks = for ((context, curves) <- results.scopes; if curves.nonEmpty) yield {
      val history = persistor.load(context)
      val curvespassed = test(context, curves, history)
      if (curvespassed.forall(_ == true)) persistor.save(context, curves)
      curvespassed
    }

    val failure = oks.flatten.count(_ == false)
    val success = oks.flatten.count(_ == true)
    val color = if (failure == 0) ansi.green else ansi.red
    log(s"${color} Summary: $success tests passed, $failure tests failed.")
  }

}


object RegressionReporter {

}

