package org.collperf
package execution



import java.io._
import collection._
import sys.process._
import utils.Tree



class MultipleJvmPerSetupExecutor(val aggregator: Aggregator, val measurer: Executor.Measurer) extends Executor {

  val runner = new JvmRunner

  def maxHeap = 2048

  def startHeap = 2048

  def defaultIndependentSamples = 9

  def run[T](setuptree: Tree[Setup[T]]): Tree[CurveData] = {
    for (setup <- setuptree) yield runSetup(setup)
  }

  private[execution] def runSetup[T](setup: Setup[T]): CurveData = {
    import setup._

    val warmups = context.goe(Key.warmupRuns, 10)
    val totalreps = context.goe(Key.benchRuns, 10)
    val independentSamples = context.goe(Key.independentSamples, defaultIndependentSamples)
    def repetitions(idx: Int): Int = {
      val is = independentSamples
      totalreps / is + (if (idx < totalreps % is) 1 else 0)
    }

    val m = measurer

    def sample(idx: Int, reps: Int): Seq[(Parameters, Seq[Long])] = runner.run(jvmflags(startHeap = startHeap, maxHeap = maxHeap)) {
      initialContext = context
      
      log.verbose(s"Sampling $reps measurements in separate JVM invocation $idx - ${context.scope}, ${context.goe(Key.curve, "")}.")

      // warmup
      customwarmup match {
        case Some(warmup) =>
          for (i <- 0 until warmups) warmup()
        case _ =>
          for (x <- gen.warmupset) {
            for (i <- Warmer(warmups, setupFor(x), teardownFor(x))) snippet(x)
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

    log.verbose(s"Running test set for ${context.scope}, curve ${context.goe(Key.curve, "")}")
    log.verbose(s"Starting $totalreps measurements across $independentSamples independent JVM runs.")

    val timeseqs = for {
      idx <- 0 until independentSamples
      reps = repetitions(idx)
    } yield sample(idx, reps)

    // ugly as hell
    val timeseq = timeseqs.reduceLeft { (accseq, timeseq) =>
      accseq zip timeseq map {
        case ((k1, x), (k2, y)) => (k1, x ++ y)
      }
    }

    def nice(ts: Seq[(Parameters, Seq[Long])]) = ts map {
      case (params, seq) => params.axisData.mkString(", ") + ": " + seq.mkString(", ")
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


object MultipleJvmPerSetupExecutor extends Executor.Factory[MultipleJvmPerSetupExecutor] {

  def apply(agg: Aggregator, m: Executor.Measurer) = new MultipleJvmPerSetupExecutor(agg, m)

}




























