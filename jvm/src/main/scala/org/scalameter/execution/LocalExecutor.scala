package org.scalameter
package execution



import org.scalameter.picklers.Pickler
import org.scalameter.utils.Tree
import scala.collection._
import scala.compat.Platform



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
class LocalExecutor[V: Pickler](val warmer: Warmer, val aggregator: Aggregator[V],
    val measurer: Measurer[V]) extends Executor[V] {
  require(!measurer.usesInstrumentedClasspath,
    s"${measurer.getClass.getName} should be run using SeparateJvmsExecutor.")

  import Key._

  override def run[T](setups: Tree[Setup[T]], reporter: Reporter[V], persistor: Persistor) = {
    // run all warmups for classloading purposes
    for (bench <- setups) {
      import bench._
      setupBeforeAll()
      for (x <- gen.warmupset) {
        customwarmup.map(_())
        for (_ <- warmer.warming(context, setupFor(x), teardownFor(x))) snippet(x)
      }
      teardownAfterAll()
    }

    // for every benchmark - do a warmup, and then measure
    super.run(setups, reporter, persistor)
  }

  def runSetup[T](bsetup: Setup[T]): CurveData[V] = {
    import bsetup._

    log.verbose(s"Running test set for ${bsetup.context.scope}, curve ${bsetup.context(dsl.curve)}")

    // run warm up
    setupBeforeAll()
    try {
      val warmups = context(exec.maxWarmupRuns)
      customwarmup match {
        case Some(warmup) =>
          for (i <- 0 until warmups) warmup()
        case _ =>
          for (x <- gen.warmupset) {
            for (i <- warmer.warming(context, setupFor(x), teardownFor(x))) snippet(x)
          }
      }

      // perform GC
      Platform.collectGarbage()

      // run tests
      val measurements = new mutable.ArrayBuffer[Measurement[V]]()
      val repetitions = context(exec.benchRuns)

      for (params <- gen.dataset) {
        val set = setupFor()
        val tear = teardownFor()
        val regen = regenerateFor(params)

        log.verbose(s"$repetitions repetitions of the snippet starting.")
        val values = measurer.measure(context, repetitions, set, tear, regen, snippet)
        log.verbose("Repetitions ended.")

        val processedValues = aggregator(values)
        val data = aggregator.data(values)
        measurements += Measurement(processedValues.value, params, data, processedValues.units)
      }
      CurveData(measurements, Map.empty, context)
    } finally {
      teardownAfterAll()
    }
  }

  override def toString = s"LocalExecutor(${aggregator.name}, ${measurer.name})"

}


object LocalExecutor extends Executor.Factory[LocalExecutor] {

  def apply[V: Pickler: PrettyPrinter](w: Warmer, a: Aggregator[V], m: Measurer[V]) =
    new LocalExecutor(w, a, m)

}












