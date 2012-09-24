package org.collperf



import collection._
import compat.Platform



object Runner {

  private val scheduled = mutable.ArrayBuffer[Benchmark[_]]()

  def schedule[T](benchmark: Benchmark[T]) {
    scheduled += benchmark
  }

  def flushSchedule(): Seq[Benchmark[_]] = {
    val result = scheduled.toVector
    scheduled.clear()
    result
  }

  def run[T](benchmark: Benchmark[T]): Result = {
    import benchmark._

    // run warm up
    customwarmup match {
      case Some(warmup) => warmup()
      case _ => for (x <- gen.warmupset) snippet(x)
    }

    // perform GC
    Platform.collectGarbage()

    // run tests
    val set = setup.orNull
    val tear = teardown.orNull
    val measurements = new mutable.ArrayBuffer[Measurement]()
    for ((x, params) <- gen.dataset) {
      if (set != null) set(x)

      val start = Platform.currentTime
      snippet(x)
      val end = Platform.currentTime
      val time = end - start
      measurements += Measurement(time, params)

      if (tear != null) tear(x)
    }

    Result(measurements, context)
  }

}
