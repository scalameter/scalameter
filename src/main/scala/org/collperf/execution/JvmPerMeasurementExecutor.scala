package org.collperf
package execution



import java.io._
import collection._
import sys.process._
import utils.Tree



class JvmPerMeasurementExecutor(val aggregator: Aggregator, val measurer: Executor.Measurer) extends Executor {

  val runner = new JvmRunner

  def maxHeap = 2048

  def startHeap = 2048

  def independentSamples = 5

  def run[T](setuptree: Tree[Setup[T]]): Tree[CurveData] = {
    for (setup <- setuptree) yield runSetup(setup)
  }

  private[execution] def runSetup[T](setup: Setup[T]): CurveData = {
    import setup._

    val warmups = context.goe(Key.warmupRuns, 1)
    val totalreps = context.goe(Key.benchRuns, 1)
    def repetitions(idx: Int): Int = {
      val is = independentSamples
      totalreps / is + (if (idx < totalreps % is) 1 else 0)
    }

    val m = measurer

    def sample(idx: Int, reps: Int): immutable.HashMap[Parameters, Seq[Long]] = runner.run(jvmflags(startHeap = startHeap, maxHeap = maxHeap)) {
      initialContext = context
      
      log.verbose(s"Sampling $reps measurements in separate JVM invocation $idx.")

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
        (params, m.measure(reps, set, tear, regen, snippet))
      }

      immutable.HashMap(observations.toSeq: _*)
    }

    log.verbose(s"Running test set for ${context.scope}")
    log.verbose(s"Starting $totalreps measurements across $independentSamples independent JVM runs.")

    val timemaps = for {
      idx <- 0 until independentSamples
      reps = repetitions(idx)
    } yield sample(idx, reps)

    val timemap = timemaps.foldLeft(immutable.HashMap[Parameters, Seq[Long]]()) { (accmap, timemap) =>
      val result = accmap.toSeq.sortBy(_._1.axisData.toList.map(_._1).toString) zip timemap.toSeq.sortBy(_._1.axisData.toList.map(_._1).toString) map {
        case ((k1, x), (k2, y)) => (k1, x ++ y)
      }
      immutable.HashMap(result: _*)
    }

    val measurements = timemap map {
      case (params, times) => Measurement(
        aggregator(times),
        params,
        aggregator.data(times)
      )
    }

    CurveData(measurements.toSeq, Map.empty, context)
  }

  override def toString = s"JvmPerSetupExecutor(${aggregator.name}, ${measurer.name})"

}


object JvmPerMeasurementExecutor extends Executor.Factory[JvmPerMeasurementExecutor] {

  def apply(agg: Aggregator, m: Executor.Measurer) = new JvmPerMeasurementExecutor(agg, m)

}




























