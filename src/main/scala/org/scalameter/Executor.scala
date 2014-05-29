package org.scalameter


import collection._
import compat._
import utils.{withGCNotification, Tree}



trait Executor {

  def run[T](setuptree: Tree[Setup[T]], reporter: Reporter, persistor: Persistor): Tree[CurveData] = {
    for (setup <- setuptree) yield {
      val exec = Option(setup.customExecutor).getOrElse(this)
      val cd = exec.runSetup(setup)
      reporter.report(cd, persistor)
      cd
    }
  }

  def runSetup[T](setup: Setup[T]): CurveData

}


object Executor {

  import Key._

  object None extends Executor {
    def runSetup[T](setup: Setup[T]): CurveData = ???
  }

  trait Factory[E <: Executor] {
    def apply(warmer: Warmer, aggregator: Aggregator, m: Measurer): E
  }

  trait Warmer extends Serializable {
    def name: String
    def warming(ctx: Context, setup: () => Any, teardown: () => Any): Foreach[Int]
  }

  object Warmer {

    case class Default() extends Warmer {
      def name = "Warmer.Default"
      def warming(ctx: Context, setup: () => Any, teardown: () => Any) = new Foreach[Int] {
        val minwarmups = ctx(exec.minWarmupRuns)
        val maxwarmups = ctx.goe(exec.maxWarmupRuns, 50)
        val covThreshold = ctx(exec.warmupCovThreshold)

        def foreach[U](f: Int => U): Unit = {
          val withgc = new utils.SlidingWindow(minwarmups)
          val withoutgc = new utils.SlidingWindow(minwarmups)
          @volatile var nogc = true

          log.verbose(s"Starting warmup.")

          withGCNotification { n =>
            nogc = false
            log.verbose("GC detected.")
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
                i = maxwarmups
              } else i += 1
            }
            log.verbose(s"Ending warmup.")
          }
        }
      }
    }

  }

}






















