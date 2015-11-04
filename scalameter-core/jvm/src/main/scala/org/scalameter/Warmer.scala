package org.scalameter



import collection._
import compat._
import utils.withGCNotification



trait Warmer extends Serializable {
  def name: String
  def warming(ctx: Context, setup: () => Any, teardown: () => Any): Foreach[Int]
}


object Warmer {

  import Key._

  case object Zero extends Warmer {
    private val foreachObj = new Foreach[Int] with Serializable {
      def foreach[U](f: Int => U): Unit = {}
    }
    def name = "Warmer.Zero"
    def warming(ctx: Context, setup: () => Any, teardown: () => Any) = foreachObj
  }

  case class Default() extends Warmer {
    def name = "Warmer.Default"
    def warming(ctx: Context, setup: () => Any, teardown: () => Any) = new Foreach[Int] {
      val minwarmups = ctx(exec.minWarmupRuns)
      val maxwarmups = ctx.goe(exec.maxWarmupRuns, 50)
      val covThreshold = ctx(exec.warmupCovThreshold)

      def foreach[U](f: Int => U): Unit = {
        var steady = false
        val withgc = new utils.SlidingWindow(minwarmups)
        val withoutgc = new utils.SlidingWindow(minwarmups)
        @volatile var nogc = true

        log.verbose(s"Starting warmup.")

        withGCNotification { n =>
          dyn.currentContext.withValue(ctx) {
            nogc = false
            log.verbose("GC detected.")
          }
        } {
          var i = 0
          while (i < maxwarmups) {

            setup()
            nogc = true
            val start = System.nanoTime
            f(i)
            val end = System.nanoTime
            val runningtime = (end - start) / 1000000.0

            if (nogc) withoutgc.add(runningtime)
            withgc.add(runningtime)
            teardown()

            val covNoGC = withoutgc.cov
            val covGC = withgc.cov

            log.verbose(f"$i. warmup run running time: $runningtime (covNoGC: ${covNoGC}%.4f, covGC: ${covGC}%.4f)")
            if ((withoutgc.size >= minwarmups && covNoGC < covThreshold) || (withgc.size >= minwarmups && covGC < covThreshold)) {
              log.verbose(s"Steady-state detected.")
              steady = true
              i = maxwarmups
            } else i += 1
          }
          if (steady) log.verbose(s"Ending warmup.")
          else log.verbose(s"Steady-state not detected.")
        }
      }
    }
  }

}
