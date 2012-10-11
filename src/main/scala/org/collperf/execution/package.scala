package org.collperf



import compat.Platform
import utils._



package object execution {

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

}