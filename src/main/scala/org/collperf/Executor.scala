package org.collperf



import collection._
import compat._
import utils.{withGCNotification, Tree}



trait Executor {

  def run[T](setups: Tree[Setup[T]]): Tree[CurveData]

}


object Executor {

  trait Factory[E <: Executor] {
    def apply(aggregator: Aggregator, m: Measurer): E

    def min = apply(Aggregator.min, new Measurer.Default)

    def max = apply(Aggregator.max, new Measurer.Default)

    def average = apply(Aggregator.average, new Measurer.Default)

    def median = apply(Aggregator.median, new Measurer.Default)

    def complete(a: Aggregator) = apply(Aggregator.complete(a), new Measurer.Default)
  }

  trait Measurer extends Serializable {
    def name: String
    def measure[T, U](measurements: Long, setup: T => Any, tear: T => Any, regen: () => T, snippet: T => Any): Seq[Long]
  }

  object Measurer {

    /** A default measurer executes the test as many times as specified and returns the sequence of measured times. */
    class Default extends Measurer {
      def name = "Measurer.Default"

      /** Returns the value used for the benchmark at `iteration`.
       *  May optionally call `regen` to obtain a new value for the benchmark.
       *  
       *  By default, the value `v` is always returned and the value for the
       *  benchmark is never regenerated.
       */
      protected def valueAt[T](iteration: Int, regen: () => T, v: T): T = v

      def measure[T, U](measurements: Long, setup: T => Any, tear: T => Any, regen: () => T, snippet: T => Any): Seq[Long] = {
        var iteration = 0
        var times = List[Long]()
        var value = regen()

        while (iteration < measurements) {
          value = valueAt(iteration, regen, value)
          setup(value)

          val start = Platform.currentTime
          snippet(value)
          val end = Platform.currentTime
          val time = end - start

          tear(value)

          times ::= time
          iteration += 1
        }

        log.verbose(s"measurements: ${times.mkString(", ")}")

        times
      }
    }

    /** A measurer that discards measurements for which it detects that GC cycles occurred.
     *  
     *  Assume that `M` measurements are requested.
     *  To prevent looping forever, after the number of measurement failed due to GC exceeds the number of successful
     *  measurements by more than `M`, the subsequent measurements are accepted irregardless of whether GC cycles occur.
     */
    class IgnoringGC extends Default {
      override def name = "Measurer.IgnoringGC"

      override def measure[T, U](measurements: Long, setup: T => Any, tear: T => Any, regen: () => T, snippet: T => Any): Seq[Long] = {
        var times = List[Long]()
        var okcount = 0
        var gccount = 0
        var ignoring = true
        var value = regen()

        while (okcount < measurements) {
          value = valueAt(okcount + gccount, regen, value)
          setup(value)

          @volatile var gc = false
          val time = withGCNotification { n =>
            gc = true
            log.verbose("GC detected.")
          } apply {
            val start = Platform.currentTime
            snippet(value)
            val end = Platform.currentTime
            end - start
          }

          tear(value)

          if (ignoring && gc) {
            gccount += 1
            if (gccount - okcount > measurements) ignoring = false
          } else {
            okcount += 1
            times ::= time
          }
        }

        log.verbose(s"${if (ignoring) "All GC time ignored" else "Some GC time recorded"}, accepted: $okcount, ignored: $gccount")
        log.verbose(s"measurements: ${times.mkString(", ")}")

        times
      }
    }

    /** A mixin measurer which causes the value for the benchmark to be reinstantiated
     *  every `frequency` measurements.
     *  Before the new value has been instantiated, a full GC cycle is invoked if `fullGC` is `true`.
     */
    trait Reinstantiation extends Default {
      def frequency: Int
      def fullGC: Boolean

      override def name = super.name + "+Reinstantiation"

      protected override def valueAt[T](iteration: Int, regen: () => T, v: T) = {
        if ((iteration + 1) % frequency == 0) {
          log.verbose("Reinstantiating benchmark value.")
          if (fullGC) Platform.collectGarbage()
          val nv = regen()
          nv
        } else v
      }
    }

  }

}











