package org.collperf



import collection._
import compat.Platform



trait Executor {

  def run[T](benchmark: Benchmark[T]): Result

}


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
class LocalExecutor(aggregate: Seq[Long] => Long) extends Executor {

  case class Warmer(warmups: Int, set: () => Any, tear: () => Any) {
    def foreach[U](f: Int => U): Unit = {
      val withgc = new utils.SlidingWindow(10)
      val withoutgc = new utils.SlidingWindow(10)
      @volatile var nogc = true

      utils.withGCNotification { n =>
        log.verbose("Garbage collection detected.")
        nogc = false
      } apply {
        set()
        var i = 0
        while (i < warmups) {
          nogc = true
          val start = Platform.currentTime
          f(i)
          val end = Platform.currentTime
          val runningtime = end - start
          if (nogc) withoutgc.add(runningtime)
          withgc.add(runningtime)
          tear()
          set()

          val covNoGC = withoutgc.cov
          val covGC = withgc.cov

          nogc = true
          log.verbose(s"$i. warmup run running time: $runningtime (covNoGC: $covNoGC, covGC: $covGC)")
          if ((withoutgc.size >= 10 && covNoGC < 0.1) || (withgc.size >= 10 && covGC < 0.1)) {
            log.verbose(s"Steady-state detected.")
            i = warmups
          } else i += 1
        }
      }
    }
  }

  def run[T](benchmark: Benchmark[T]): Result = {
    import benchmark._

    val set = setup.orNull
    val tear = teardown.orNull

    // run warm up
    val warmups = context.getOrElse[Int](Key.warmupRuns, 1)
    customwarmup match {
      case Some(warmup) =>
        for (i <- 0 until warmups) warmup()
      case _ =>
        for (x <- gen.warmupset) {
          val s = () => if (set != null) set(x)
          val t = () => if (tear != null) tear(x)
          for (i <- Warmer(warmups, s, t)) snippet(x)
        }
    }

    // perform GC
    Platform.collectGarbage()

    // run tests
    val measurements = new mutable.ArrayBuffer[Measurement]()
    val repetitions = context.getOrElse[Int](Key.benchRuns, 1)
    for ((x, params) <- gen.dataset) {
      var iteration = 0
      var times = List[Long]()
      while (iteration < repetitions) {
        if (set != null) set(x)

        val start = Platform.currentTime
        snippet(x)
        val end = Platform.currentTime
        val time = end - start

        if (tear != null) tear(x)

        times ::= time
        iteration += 1
      }

      val processedTime = aggregate(times)
      measurements += Measurement(processedTime, params)
    }

    Result(measurements, context)
  }

}


object LocalExecutor {

  def min = new LocalExecutor(_.min)

  def median = new LocalExecutor({
    xs =>
    val sorted = xs.sorted
    sorted(sorted.size / 2)
  })

  def average = new LocalExecutor(xs => xs.sum / xs.size)

}


object NewJVMExecutor extends Executor {

  def run[T](benchmark: Benchmark[T]): Result = {
    // TODO

    null
  }

}





