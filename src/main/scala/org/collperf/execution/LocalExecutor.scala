package org.collperf
package execution



import collection._
import compat.Platform



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
class LocalExecutor(val aggregator: Aggregator) extends Executor {

  case class Warmer(maxwarmups: Int, setup: () => Any, teardown: () => Any) {
    def foreach[U](f: Int => U): Unit = {
      val withgc = new utils.SlidingWindow(10)
      val withoutgc = new utils.SlidingWindow(10)
      @volatile var nogc = true

      log.verbose(s"Starting warmup.")

      utils.withGCNotification { n =>
        nogc = false
        log.verbose("Garbage collection detected.")
      } apply {
        setup()
        var i = 0
        while (i < maxwarmups) {
          nogc = true

          val start = Platform.currentTime
          f(i)
          val end = Platform.currentTime
          val runningtime = end - start

          if (nogc) withoutgc.add(runningtime)
          withgc.add(runningtime)
          teardown()
          setup()

          val covNoGC = withoutgc.cov
          val covGC = withgc.cov

          log.verbose(s"$i. warmup run running time: $runningtime (covNoGC: $covNoGC, covGC: $covGC)")
          if ((withoutgc.size >= 10 && covNoGC < 0.1) || (withgc.size >= 10 && covGC < 0.1)) {
            log.verbose(s"Steady-state detected.")
            i = maxwarmups
          } else i += 1
        }
        log.verbose(s"Ending warmup.")
      }
    }
  }

  def run[T](setups: Seq[Setup[T]]) = {
    // run all warmups for classloading purposes
    for (bench <- setups) {
      import bench._
      for (x <- gen.warmupset) {
        val warmups = context.goe(Key.warmupRuns, 1)
        for (_ <- Warmer(warmups, setupFor(x), teardownFor(x))) snippet(x)
      }
    }

    // for every benchmark - do a warmup, and then measure
    for (bench <- setups) yield {
      runSingle(bench)
    }
  }

  private def runSingle[T](benchmark: Setup[T]): CurveData = {
    import benchmark._

    // run warm up
    val warmups = context.goe(Key.warmupRuns, 1)
    customwarmup match {
      case Some(warmup) =>
        for (i <- 0 until warmups) warmup()
      case _ =>
        for (x <- gen.warmupset) {
          for (i <- Warmer(warmups, setupFor(x), teardownFor(x))) snippet(x)
        }
    }

    // perform GC
    Platform.collectGarbage()

    // run tests
    val measurements = new mutable.ArrayBuffer[Measurement]()
    val repetitions = context.goe(Key.benchRuns, 1)
    for ((x, params) <- gen.dataset) {
      val set = setupFor(x)
      val tear = teardownFor(x)
      var iteration = 0
      var times = List[Long]()
      while (iteration < repetitions) {
        set()

        val start = Platform.currentTime
        snippet(x)
        val end = Platform.currentTime
        val time = end - start

        tear()

        times ::= time
        iteration += 1
      }

      val processedTime = aggregator(times)
      val data = aggregator.data(times)
      measurements += Measurement(processedTime, params, data)
    }

    CurveData(measurements, Map.empty, context + (Key.aggregator -> aggregator.name))
  }

}


object LocalExecutor {

  def apply(a: Aggregator) = new LocalExecutor(a)

}












