package org.scalameter
package execution



import collection._
import compat.Platform
import utils.Tree



/** Runs warmups until the maximum number of warmups is done,
 *  or the running times have stabilized. After that, it runs
 *  the tests the specified number of times and collects
 *  the results using an `aggregate` function.
 * 
 *  Stabilization is detected by tracking the running times
 *  for which there may have been and those for which there
 *  was no garbage collection.
 *  When either of the two running times stabilizes, we consider
 *  the JVM warmed up for the snippet.
 * 
 *  We do this by registering on GC events instead of invoking
 *  `Platform.collectGarbage`, since usually the time to invoke
 *  the snippet is less than the time to perform full GC, and
 *  most triggered GC cycles are fast because they collect only
 *  the young generation.
 */
class LocalExecutor(val warmer: Executor.Warmer, val aggregator: Aggregator, val measurer: Measurer) extends Executor {

  import Key._

  override def run[T](setups: Tree[Setup[T]], reporter: Reporter, persistor: Persistor) = {
    // run all warmups for classloading purposes
    for (bench <- setups) {
      import bench._
      setupBeforeAll()
      for (x <- gen.warmupset) {
        val warmups = context(exec.maxWarmupRuns)
        customwarmup.map(_())
        for (_ <- warmer.warming(context, setupFor(x), teardownFor(x))) snippet(x)
      }
      teardownAfterAll()
    }

    // for every benchmark - do a warmup, and then measure
    for (bench <- setups) yield {
      val cd = runSetup(bench)
      reporter.report(cd, persistor)
      cd
    }
  }

  def runSetup[T](bsetup: Setup[T]): CurveData = {
    import bsetup._

    log.verbose(s"Running test set for ${bsetup.context.scope}, curve ${bsetup.context(dsl.curve)}")

    // run warm up
    setupBeforeAll()
    val warmups = context(exec.maxWarmupRuns)
    customwarmup match {
      case Some(warmup) =>
        for (i <- 0 until warmups) warmup()
      case _ =>
        for (x <- gen.warmupset) {
          for (i <- warmer.warming(context, setupFor(x), teardownFor(x))) snippet(x)
        }
    }
    teardownAfterAll()

    // perform GC
    Platform.collectGarbage()

    // run tests
    val measurements = new mutable.ArrayBuffer[Measurement]()
    val repetitions = context(exec.benchRuns)

    setupBeforeAll()
    for (params <- gen.dataset) {
      val set = setupFor()
      val tear = teardownFor()
      val regen = regenerateFor(params)

      log.verbose(s"$repetitions repetitions of the snippet starting.")
      val values = measurer.measure(context, repetitions, set, tear, regen, snippet)
      log.verbose("Repetitions ended.")

      val processedValues = aggregator(values)
      val data = aggregator.data(values)
      measurements += Measurement(processedValues, params, data, measurer.units)
    }
    teardownAfterAll()

    CurveData(measurements, Map.empty, context)
  }

  override def toString = s"LocalExecutor(${aggregator.name}, ${measurer.name})"

}


object LocalExecutor extends Executor.Factory[LocalExecutor] {

  def apply(w: Executor.Warmer, a: Aggregator, m: Measurer) = new LocalExecutor(w, a, m)

}












