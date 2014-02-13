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

    val warmups = context(exec.maxWarmupRuns)
    val totalreps = context(exec.benchRuns)
    val independentSamples = context(exec.independentSamples)
    def repetitions(idx: Int): Int = {
      val is = independentSamples
      totalreps / is + (if (idx < totalreps % is) 1 else 0)
    }

    val m = measurer
    val w = warmer
    val jvmContext = createJvmContext(context)

    def sample(idx: Int, reps: Int): Seq[(Parameters, Seq[Double])] = runner.run(jvmContext) {
      dyn.initialContext.value = context
      
      log.verbose(s"Sampling $reps measurements in separate JVM invocation $idx - ${context.scope}, ${context(dsl.curve)}.")

      // warmup
      setupBeforeAll()
      customwarmup match {
        case Some(warmup) =>
          for (i <- 0 until warmups) warmup()
        case _ =>
          for (x <- gen.warmupset) {
            for (i <- w.warming(context, setupFor(x), teardownFor(x))) snippet(x)
          }
      }
      teardownAfterAll()

      // perform GC
      compat.Platform.collectGarbage()

      // measure
      setupBeforeAll()
      val observations = new mutable.ArrayBuffer[(Parameters, Seq[Double])]()
      for (params <- gen.dataset) {
        val set = setupFor()
        val tear = teardownFor()
        val regen = regenerateFor(params)
        observations += (params -> m.measure(context, reps, set, tear, regen, snippet))
      }
      teardownAfterAll()

      observations
    }

    def sampleReport(idx: Int, reps: Int): Seq[(Parameters, Seq[Double])] = try {
      sample(idx, reps)
    } catch {
      case e: Exception =>
        log.error(s"Error running separate JVM: $e")
        log.error(s"Classpath: ${sys.props("java.class.path")}")
        throw e
    }

    log.verbose(s"Running test set for ${context.scope}, curve ${context(dsl.curve)}")
    log.verbose(s"Starting $totalreps measurements across $independentSamples independent JVM runs.")

    val valueseqs = for {
      idx <- 0 until independentSamples
      reps = repetitions(idx)
    } yield sampleReport(idx, reps)

    val valueseq = valueseqs.reduceLeft { (accseq, vs) =>
      accseq zip vs map {
        case ((k1, x), (k2, y)) => (k1, x ++ y)
      }
    }

    def nice(vs: Seq[(Parameters, Seq[Double])]) = vs map {
      case (params, seq) => params.axisData.mkString(", ") + ": " + seq.map(t => f"$t%.3f").mkString(", ")
    } mkString("\n")

    log.verbose(s"Obtained measurements:\n${nice(valueseq)}")

    val measurements = valueseq map {
      case (params, values) => Measurement(
        aggregator(values),
        params,
        aggregator.data(values),
        m.units
      )
    }

    CurveData(measurements.toSeq, Map.empty, context)
  }

  override def toString = s"MultipleJvmPerSetupExecutor(${aggregator.name}, ${measurer.name})"

}


object SeparateJvmsExecutor extends Executor.Factory[SeparateJvmsExecutor] {

  def apply(w: Executor.Warmer, agg: Aggregator, m: Executor.Measurer) = new SeparateJvmsExecutor(w, agg, m)

}




























