package org.collperf



import collection._
import compat.Platform



trait Executor {

  def run[T](benchmark: Benchmark[T]): Result

}


class LocalExecutor(aggregate: Seq[Long] => Long) extends Executor {

  def run[T](benchmark: Benchmark[T]): Result = {
    import benchmark._

    // run warm up
    val warmups = context.getOrElse[Int](Key.warmupRuns, 1)
    customwarmup match {
      case Some(warmup) =>
        for (i <- 0 until warmups) warmup()
      case _ =>
        for (x <- gen.warmupset) {
          for (i <- 0 until warmups) {
            val start = Platform.currentTime
            snippet(x)
            val end = Platform.currentTime
            log.verbose(i + ". warmup run running time: " + (end - start))
          }
        }
    }

    // perform GC
    Platform.collectGarbage()

    // run tests
    val set = setup.orNull
    val tear = teardown.orNull
    val measurements = new mutable.ArrayBuffer[Measurement]()
    val repetitions = context.getOrElse[Int](Key.benchRuns, 1)
    for ((x, params) <- gen.dataset) {
      if (set != null) set(x)

      var iteration = 0
      var times = List[Long]()
      while (iteration < repetitions) {
        val start = Platform.currentTime
        snippet(x)
        val end = Platform.currentTime
        val time = end - start

        times ::= time
        iteration += 1
      }

      val processedTime = aggregate(times)
      measurements += Measurement(processedTime, params)

      if (tear != null) tear(x)
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





