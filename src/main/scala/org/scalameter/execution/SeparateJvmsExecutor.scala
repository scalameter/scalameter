package org.scalameter
package execution



import java.io._
import collection._
import sys.process._
import utils.Tree



/** Runs multiple JVM instances per each setup and aggregates all the results together.
 *
 *  This produces more stable results, as the performance related effects of each JVM instantiation
 *  are averaged.
 */
class SeparateJvmsExecutor(val warmer: Executor.Warmer, val aggregator: Aggregator, val measurer: Executor.Measurer) extends Executor {

  import Key._

  val runner = new JvmRunner

  def runSetup[T](setup: Setup[T]): CurveData = {
    import setup._

    val warmups = context.goe(exec.maxWarmupRuns, 10)
    val totalreps = context.goe(exec.benchRuns, 10)
    val independentSamples = context.goe(exec.independentSamples, 9)
    def repetitions(idx: Int): Int = {
      val is = independentSamples
      totalreps / is + (if (idx < totalreps % is) 1 else 0)
    }

    val m = measurer
    val w = warmer
    val jvmContext = createJvmContext(context)

    def sample(idx: Int, reps: Int): Seq[(Parameters, Seq[Double])] = runner.run(jvmContext) {
      dyn.initialContext.value = context
      
      log.verbose(s"Sampling $reps measurements in separate JVM invocation $idx - ${context.scope}, ${context.goe(dsl.curve, "")}.")

      // warmup
      customwarmup match {
        case Some(warmup) =>
          for (i <- 0 until warmups) warmup()
        case _ =>
          for (x <- gen.warmupset) {
            for (i <- w.warming(context, setupFor(x), teardownFor(x))) snippet(x)
          }
      }

      // measure
      val observations = for (params <- gen.dataset) yield {
        val set = setupFor()
        val tear = teardownFor()
        val regen = regenerateFor(params)
        (params, m.measure(context, reps, set, tear, regen, snippet))
      }

      observations.toBuffer
    }

    def sampleReport(idx: Int, reps: Int): Seq[(Parameters, Seq[Double])] = try {
      sample(idx, reps)
    } catch {
      case e: Exception =>
        log.error(s"Error running separate JVM: $e")
        log.error(s"Classpath: ${sys.props("java.class.path")}")
        throw e
    }

    log.verbose(s"Running test set for ${context.scope}, curve ${context.goe(dsl.curve, "")}")
    log.verbose(s"Starting $totalreps measurements across $independentSamples independent JVM runs.")

    val timeseqs = for {
      idx <- 0 until independentSamples
      reps = repetitions(idx)
    } yield sampleReport(idx, reps)

    // ugly as hell
    val timeseq = timeseqs.reduceLeft { (accseq, timeseq) =>
      accseq zip timeseq map {
        case ((k1, x), (k2, y)) => (k1, x ++ y)
      }
    }

    def nice(ts: Seq[(Parameters, Seq[Double])]) = ts map {
      case (params, seq) => params.axisData.mkString(", ") + ": " + seq.map(t => f"$t%.3f").mkString(", ")
    } mkString("\n")

    log.verbose(s"Obtained measurements:\n${nice(timeseq)}")

    val measurements = timeseq map {
      case (params, times) => Measurement(
        aggregator(times),
        params,
        aggregator.data(times)
      )
    }

    CurveData(measurements.toSeq, Map.empty, context)
  }

  override def toString = s"MultipleJvmPerSetupExecutor(${aggregator.name}, ${measurer.name})"

}


object SeparateJvmsExecutor extends Executor.Factory[SeparateJvmsExecutor] {

  def apply(w: Executor.Warmer, agg: Aggregator, m: Executor.Measurer) = new SeparateJvmsExecutor(w, agg, m)

}




























