package org.scalameter



import collection._
import compat._
import utils.{withGCNotification, Tree}



trait Executor {

  def run[T](setuptree: Tree[Setup[T]], reporter: Reporter, persistor: Persistor): Tree[CurveData] = {
    for (setup <- setuptree) yield {
      val cd = runSetup(setup)
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
        val minwarmups = ctx.goe(exec.minWarmupRuns, 10)
        val maxwarmups = ctx.goe(exec.maxWarmupRuns, 50)
        val covThreshold = ctx.goe(exec.warmupCovThreshold, 0.1)

        def foreach[U](f: Int => U): Unit = {
          val withgc = new utils.SlidingWindow(minwarmups)
          val withoutgc = new utils.SlidingWindow(minwarmups)
          @volatile var nogc = true

          log.verbose(s"Starting warmup.")

          withGCNotification { n =>
            nogc = false
            log.verbose("GC detected.")
          } apply {
            setup()
            var i = 0
            while (i < maxwarmups) {
              nogc = true

              val start = System.nanoTime
              f(i)
              val end = System.nanoTime
              val runningtime = (end - start) / 1000000.0

              if (nogc) withoutgc.add(runningtime)
              withgc.add(runningtime)
              teardown()
              setup()

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

  trait Measurer extends Serializable {
    def name: String
    def measure[T, U](context: Context, measurements: Int, setup: T => Any, tear: T => Any, regen: () => T, snippet: T => Any): Seq[Double]
  }

  object Measurer {

    /** Mixin for measurers whose benchmarked value is based on the current iteration. */
    trait IterationBasedValue extends Measurer {

      /** Returns the value used for the benchmark at `iteration`.
       *  May optionally call `regen` to obtain a new value for the benchmark.
       *  
       *  By default, the value `v` is always returned and the value for the
       *  benchmark is never regenerated.
       */
      protected def valueAt[T](context: Context, iteration: Int, regen: () => T, v: T): T = v

    }

    /** A default measurer executes the test as many times as specified and returns the sequence of measured times. */
    class Default extends Measurer with IterationBasedValue {
      def name = "Measurer.Default"

      def measure[T, U](context: Context, measurements: Int, setup: T => Any, tear: T => Any, regen: () => T, snippet: T => Any): Seq[Double] = {
        var iteration = 0
        var times = List[Double]()
        var value = regen()

        while (iteration < measurements) {
          value = valueAt(context, iteration, regen, value)
          setup(value)

          val start = System.nanoTime
          snippet(value)
          val end = System.nanoTime
          val time = (end - start) / 1000000.0

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
    class IgnoringGC extends Measurer with IterationBasedValue {
      override def name = "Measurer.IgnoringGC"

      def measure[T, U](context: Context, measurements: Int, setup: T => Any, tear: T => Any, regen: () => T, snippet: T => Any): Seq[Double] = {
        var times = List[Double]()
        var okcount = 0
        var gccount = 0
        var ignoring = true
        var value = regen()

        while (okcount < measurements) {
          value = valueAt(context, okcount + gccount, regen, value)
          setup(value)

          @volatile var gc = false
          val time = withGCNotification { n =>
            gc = true
            log.verbose("GC detected.")
          } apply {
            val start = System.nanoTime
            snippet(value)
            val end = System.nanoTime
            (end - start) / 1000000.0
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
     *  every `Key.exec.reinstantiation.frequency` measurements.
     *  Before the new value has been instantiated, a full GC cycle is invoked if `Key.exec.reinstantiation.fullGC` is `true`.
     */
    trait PeriodicReinstantiation extends IterationBasedValue {
      import exec.reinstantiation._

      abstract override def name = s"${super.name}+PeriodicReinstantiation"

      protected override def valueAt[T](context: Context, iteration: Int, regen: () => T, v: T) = {
        val freq = context.goe(frequency, 10)
        val fullgc = context.goe(fullGC, false)

        if ((iteration + 1) % freq == 0) {
          log.verbose("Reinstantiating benchmark value.")
          if (fullgc) Platform.collectGarbage()
          val nv = regen()
          nv
        } else v
      }
    }

    /** A mixin measurer which detects outliers (due to an undetected GC or JIT) and requests additional measurements to replace them.
     *  Outlier elimination can also eliminate some pretty bad allocation patterns in some cases.
     *  Only outliers from above are considered.
     *
     *  When detecting an outlier, up to `Key.exec.outliers.suspectPercent`% (with a minimum of `1`) of worst times will be considered.
     *  For example, given `Key.exec.outliers.suspectPercent = 25` the times:
     *
     *  {{{
     *      10, 11, 10, 12, 11, 11, 10, 11, 44
     *  }}}
     *
     *  times `12` and `44` are considered for outlier elimination.
     *
     *  Given the times:
     *  
     *  {{{
     *      10, 12, 14, 55
     *  }}}
     *  
     *  the time `55` will be considered for outlier elimination.
     *
     *  A potential outlier (suffix) is removed if removing it increases the coefficient of variance by at least `Key.exec.outliers.covMultiplier` times.
     */
    trait OutlierElimination extends Measurer {

      import exec.outliers._

      abstract override def name = s"${super.name}+OutlierElimination"

      abstract override def measure[T, U](context: Context, measurements: Int, setup: T => Any, tear: T => Any, regen: () => T, snippet: T => Any): Seq[Double] = {
        import utils.Statistics._

        var results = super.measure(context, measurements, setup, tear, regen, snippet).sorted
        val suspectp = context.goe(suspectPercent, 25)
        val covmult = context.goe(covMultiplier, 2.0)
        val suspectnum = math.max(1, results.length * suspectp / 100)
        var retleft = context.goe(retries, 8)

        def outlierSuffixLength(rs: Seq[Double]): Int = {
          var minlen = 1
          while (minlen <= suspectnum) {
            val cov = CoV(rs)
            val covinit = CoV(rs.dropRight(minlen))
            val confirmed = if (covinit != 0.0) cov > covmult * covinit
              else mean(rs.takeRight(minlen)) > 1.2 * mean(rs.dropRight(minlen))

            if (confirmed) return minlen
            else minlen += 1
          }
          0
        }

        def outlierExists(rs: Seq[Double]) = outlierSuffixLength(rs) > 0

        var best = results
        while (outlierExists(results) && retleft > 0) {
          val suffixlen = outlierSuffixLength(results)
          log.verbose(s"Detected $suffixlen outlier(s): ${results.mkString(", ")}")
          results = (results.dropRight(suffixlen) ++ super.measure(context, suffixlen, setup, tear, regen, snippet)).sorted
          if (CoV(results) < CoV(best)) best = results
          retleft -= 1
        }

        log.verbose("After outlier elimination: " + best.mkString(", "))
        best
      }
    }

    /** A measurer which adds noise to the measurement.
     *
     *  @define noise This measurer makes the regression tests more solid. While certain forms of
     *  gradual regressions are harder to detect, the measurements become less
     *  susceptible to actual randomness, because adding artificial noise increases
     *  the confidence intervals.
     */
    trait Noise extends Measurer {

      import exec.noise._

      def noiseFunction(observations: Seq[Double], magnitude: Double): Double => Double

      abstract override def measure[T, U](context: Context, measurements: Int, setup: T => Any, tear: T => Any, regen: () => T, snippet: T => Any): Seq[Double] = {
        val observations = super.measure(context, measurements, setup, tear, regen, snippet)
        val magni = context.goe(magnitude, 0.0)
        val noise = noiseFunction(observations, magni)
        val withnoise = observations map {
          x => (x + 0.49 + noise(x))
        }

        log.verbose("After applying noise: " + withnoise.mkString(", "))

        withnoise
      }

    }

    import utils.Statistics.clamp

    /** A mixin measurer which adds an absolute amount of Gaussian noise to the measurement.
     *  
     *  A random value is sampled from a Gaussian distribution for each measurement `x`.
     *  This value is then multiplied with `Key.noiseMagnitude` and added to the measurement.
     *  The default value for the noise magnitude is `0.0` - it has to be set manually
     *  for tests requiring artificial noise.
     *  The resulting value is clamped into the range `x - magnitude, x + magnitude`.
     *  
     *  $noise
     */
    trait AbsoluteNoise extends Noise {

      abstract override def name = s"${super.name}+AbsoluteNoise"

      def noiseFunction(observations: Seq[Double], m: Double) = (x: Double) => {
        clamp(m * util.Random.nextGaussian(), -m, +m)
      }

    }

    /** A mixin measurer which adds an amount of Gaussian noise to the measurement relative
     *  to its mean.
     * 
     *  An observations sequence mean `m` is computed.
     *  A random Gaussian value is sampled for each measurement `x` in the observations sequence.
     *  It is multiplied with `m / 10.0` times `Key.noiseMagnitude` (default `0.0`).
     *  Call this multiplication factor `f`.
     *  The resulting value is clamped into the range `x - f, x + f`.
     *
     *  The bottomline is - a `1.0` noise magnitude is a variation of `10%` of the mean.
     *
     *  $noise
     */
    trait RelativeNoise extends Noise {

      abstract override def name = s"${super.name}+RelativeNoise"

      def noiseFunction(observations: Seq[Double], magnitude: Double) = {
        val m = utils.Statistics.mean(observations)
        (x: Double) => {
          val f = m / 10.0 * magnitude
          clamp(f * util.Random.nextGaussian(), -f, +f)
        }
      }

    }

  }

}






















