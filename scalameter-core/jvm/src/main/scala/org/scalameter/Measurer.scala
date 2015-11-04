package org.scalameter



import org.scalameter.execution.invocation.InvocationCountMatcher
import org.scalameter.utils.withGCNotification
import scala.collection._
import scala.compat._
import scala.runtime.BoxesRunTime
import scala.util.matching.Regex



trait Measurer[V] extends Serializable { self =>
  def name: String

  def measure[T](context: Context, measurements: Int, setup: T => Any,
    tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[V]]

  def map[U](f: Quantity[V] => Quantity[U]) = new Measurer[U] {
    def name = self.name

    def measure[T](context: Context, measurements: Int, setup: T => Any,
      tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[U]] =
      self.measure(context, measurements, setup, tear, regen, snippet).map(f)

    override def usesInstrumentedClasspath: Boolean = self.usesInstrumentedClasspath

    override def prepareContext(context: Context): Context = self.prepareContext(context)

    override def beforeExecution(context: Context): Unit = self.beforeExecution(context)

    override def afterExecution(context: Context): Unit = self.afterExecution(context)
  }

  /** Indicates if a measurer uses instrumented classpath -
   *  if `true` measurer must be run using an executor that spawns separate JVMs.
   */
  def usesInstrumentedClasspath: Boolean = false

  /** Modifies the initial test context.
   *
   *  This method is invoked before the `PerformanceTest` object's constructor is invoked.
   *  The key-value pairs that the [[org.scalameter.Measurer]] adds to
   *  the [[org.scalameter.Context]] in this method are visible to all
   *  the test snippets within the `PerformanceTest` class.
   *
   *  Most measurers do not need to add any specific keys,
   *  so the default implementation just returns the `context`.
   */
  def prepareContext(context: Context): Context = context

  /** Does some side effects before execution of all benchmarks in a performance test.
   *
   *  This method is invoked in the `PerformanceTest` `executeTests` method
   *  just before execution of any benchmarks.
   *
   *  Most measurers do not need add additional context keys in [[prepareContext]],
   *  so the default implementation just does nothing.
   */
  def beforeExecution(context: Context): Unit = ()

  /** Does some final cleanup after execution of all benchmarks in a performance test.
   *
   *  This method is invoked in the `PerformanceTest` `executeTests` method
   *  just after execution of all benchmarks.
   *
   *  Most measurers do not need to do any side effects in [[beforeExecution]],
   *  so the default implementation just does nothing.
   */
  def afterExecution(context: Context): Unit = ()
}


object Measurer {

  import Key._

  /** Measurer that measures nothing. */
  def None[V] = new Measurer[V] {
    def name = "None"
    def measure[T](context: Context, measurements: Int, setup: T => Any,
      tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[V]] = ???
  }

  trait Timer extends Measurer[Double]

  /** Mixin for measurers whose benchmarked value is based on the current iteration. */
  trait IterationBasedValue {

    /** Returns the value used for the benchmark at `iteration`.
     *  May optionally call `regen` to obtain a new value for the benchmark.
     *  
     *  By default, the value `v` is always returned and the value for the
     *  benchmark is never regenerated.
     */
    protected def valueAt[T](context: Context, iteration: Int, regen: () => T, v: T): T = v

  }

  object Default {
    def apply() = new Default

    def withNanos() = new Default map { case Quantity(v, _) => 
      Quantity(v * 1000000.0, "ns") 
    }
  }

  /** A default measurer executes the test as many times as specified and returns the sequence of measured times. */
  class Default extends Timer with IterationBasedValue {
    def name = "Measurer.Default"

    def measure[T](context: Context, measurements: Int, setup: T => Any,
      tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[Double]] = {
      var iteration = 0
      val times = mutable.ListBuffer.empty[Quantity[Double]]
      var value = regen()

      while (iteration < measurements) {
        value = valueAt(context, iteration, regen, value)
        setup(value)

        val start = System.nanoTime
        snippet(value)
        val end = System.nanoTime
        val time = Quantity((end - start) / 1000000.0, "ms")

        tear(value)

        times += time
        iteration += 1
      }

      log.verbose(s"measurements: ${times.mkString(", ")}")

      times.result()
    }
  }

  /** A measurer that discards measurements for which it detects that GC cycles occurred.
   *  
   *  Assume that `M` measurements are requested.
   *  To prevent looping forever, after the number of measurement failed due to GC exceeds the number of successful
   *  measurements by more than `M`, the subsequent measurements are accepted irregardless of whether GC cycles occur.
   */
  class IgnoringGC extends Timer with IterationBasedValue {
    override def name = "Measurer.IgnoringGC"

    def measure[T](context: Context, measurements: Int, setup: T => Any,
      tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[Double]] = {
      val times = mutable.ListBuffer.empty[Quantity[Double]]
      var okcount = 0
      var gccount = 0
      var ignoring = true
      var value = regen()

      while (okcount < measurements) {
        value = valueAt(context, okcount + gccount, regen, value)
        setup(value)

        @volatile var gc = false
        val time = withGCNotification { n =>
          dyn.currentContext.withValue(context) {
            gc = true
            log.verbose("GC detected.")
          }
        } {
          val start = System.nanoTime
          snippet(value)
          val end = System.nanoTime
          Quantity((end - start) / 1000000.0, "ms")
        }

        tear(value)

        if (ignoring && gc) {
          gccount += 1
          if (gccount - okcount > measurements) ignoring = false
        } else {
          okcount += 1
          times += time
        }
      }

      log.verbose(s"${if (ignoring) "All GC time ignored"
        else "Some GC time recorded"}, accepted: $okcount, ignored: $gccount")
      log.verbose(s"measurements: ${times.mkString(", ")}")

      times.result()
    }
  }

  /** A mixin measurer which causes the value for the benchmark to be reinstantiated
   *  every `Key.exec.reinstantiation.frequency` measurements.
   *  Before the new value has been instantiated, a full GC cycle is invoked if `Key.exec.reinstantiation.fullGC` is `true`.
   */
  trait PeriodicReinstantiation[V] extends Measurer[V] with IterationBasedValue {
    import exec.reinstantiation._

    abstract override def name = s"${super.name}+PeriodicReinstantiation"

    def defaultFrequency = 10
    def defaultFullGC = false

    protected override def valueAt[T](context: Context, iteration: Int, regen: () => T, v: T) = {
      val freq = context.goe(frequency, defaultFrequency)
      val fullgc = context.goe(fullGC, defaultFullGC)

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
  trait OutlierElimination[V] extends Measurer[V] {

    import exec.outliers._

    implicit def numeric: Numeric[V]

    abstract override def name = s"${super.name}+OutlierElimination"

    def eliminateLow = false

    def covMultiplierModifier = 1.0

    abstract override def measure[T](context: Context, measurements: Int, setup: T => Any,
      tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[V]] = {
      import utils.Statistics._
      import Numeric.Implicits._

      implicit val ord = Ordering.by((q: Quantity[V]) => q.value)
      var results = super.measure(context, measurements, setup, tear, regen, snippet).sorted
      val suspectp = context(suspectPercent)
      val covmult = context(covMultiplier)
      val suspectnum = math.max(1, results.length * suspectp / 100)
      var retleft = context(retries)

      def suffixLength(rs: Seq[Double]): Int = {
        import utils.Statistics._

        var minlen = 1
        while (minlen <= suspectnum) {
          val cov = CoV(rs)
          val covinit = CoV(rs.dropRight(minlen))
          val confirmed = if (covinit != 0.0) cov > covmult * covinit * covMultiplierModifier
            else mean(rs.takeRight(minlen)) > 1.2 * mean(rs.dropRight(minlen))
  
          if (confirmed) return minlen
          else minlen += 1
        }
  
        0
      }

      def outlierExists(rs: Seq[Double]) = {
        suffixLength(rs) > 0 || (eliminateLow && suffixLength(rs.reverse) > 0)
      }

      var best = results
      while (outlierExists(results.map(_.value.toDouble())) && retleft > 0) {
        val prefixlen = suffixLength(results.reverse.map(_.value.toDouble()))
        val suffixlen = suffixLength(results.map(_.value.toDouble()))
        val formatted = results.map(t => f"${t.value.toDouble()}%.3f")
        log.verbose(s"Detected $suffixlen outlier(s): ${formatted.mkString(", ")}")
        results = {
          if (eliminateLow) (
            super.measure(context, prefixlen, setup, tear, regen, snippet) ++
            results.drop(prefixlen).dropRight(suffixlen) ++
            super.measure(context, suffixlen, setup, tear, regen, snippet)
          ).sorted else (results.dropRight(suffixlen) ++
            super.measure(context, suffixlen, setup, tear, regen, snippet)).sorted
        }
        if (CoV(results.map(_.value.toDouble())) < CoV(best.map(_.value.toDouble()))) best = results
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
  trait Noise extends Measurer[Double] {

    import exec.noise._

    def noiseFunction(observations: Seq[Double], magnitude: Double): Double => Double

    abstract override def measure[T](context: Context, measurements: Int, setup: T => Any,
      tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[Double]] = {
      val observations = super.measure(context, measurements, setup, tear, regen, snippet)
      val magni = context(magnitude)
      val noise = noiseFunction(observations.map(_.value), magni)
      val withnoise = observations map {
        x => x.copy(value = x.value + noise(x.value))
      }

      val formatted = withnoise.map(t => f"${t.value}%.3f")
      log.verbose("After applying noise: " + formatted.mkString(", "))

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

  abstract class BaseMemoryFootprint extends Measurer[Double] {
    def name = "Measurer.MemoryFootprint"

    def measure[T](context: Context, measurements: Int, setup: T => Any,
      tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[Double]] = {
      val runtime = Runtime.getRuntime
      var iteration = 0
      val memories = mutable.ListBuffer.empty[Quantity[Double]]
      var value: T = null.asInstanceOf[T]
      var obj: Any = null.asInstanceOf[Any]

      while (iteration < measurements) {
        value = null.asInstanceOf[T]
        obj = null.asInstanceOf[T]

        Platform.collectGarbage()
        val membefore = runtime.totalMemory - runtime.freeMemory

        value = regen()
        setup(value)

        obj = snippet(value)

        tear(value)
        value = null.asInstanceOf[T]
          
        Platform.collectGarbage()
        val memafter = runtime.totalMemory - runtime.freeMemory

        val memory = Quantity((memafter - membefore) / 1000.0, "kB")

        memories += memory
        iteration += 1
      }

      log.verbose("Measurements: " + memories.mkString(", "))

      memories.result()
    }

  }

  /** Measures the total memory footprint of an object created by the benchmarking snippet.
   *
   *  Eliminates outliers.
   */
  class MemoryFootprint extends BaseMemoryFootprint with OutlierElimination[Double] {
    override def eliminateLow = true

    def numeric: Numeric[Double] = implicitly[Numeric[Double]]
  }

  class GarbageCollectionCycles extends Measurer[Int] {
    def name = "Measurer.GarbageCollectionCycles"

    def measure[T](context: Context, measurements: Int, setup: T => Any,
      tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[Int]] = {
      var iteration = 0
      val gcs = mutable.ListBuffer.empty[Quantity[Int]]
      var value: T = null.asInstanceOf[T]
      @volatile var count = 0

      while (iteration < measurements) {
        value = regen()
        count = 0

        setup(value)
        Platform.collectGarbage()

        utils.withGCNotification { n =>
          dyn.currentContext.withValue(context) {
            log.verbose("GC detected.")
            count += 1
          }
        } {
          snippet(value)
        }

        tear(value)
        value = null.asInstanceOf[T]

        gcs += Quantity(count, "#")
        iteration += 1
      }

      log.verbose("Measurements: " + gcs.mkString(", "))
      gcs.result()
    }
  }

  type Primitive = Boolean with Char with Byte
    with Short with Int with Long with Float with Double

  /** Counts autoboxed by a Scala compiler values.
   *
   *  @param primitives primitive types whose autoboxing will be counted.
   */
  case class BoxingCount(primitives: Class[_ >: Primitive]*) extends InvocationCount {
    val matcher: InvocationCountMatcher = {
      import InvocationCountMatcher._

      val primitiveToBoxedMethod: Map[Class[_ >: Primitive], String] = Map(
        classOf[Boolean] -> "boxToBoolean",
        classOf[Char] -> "boxToCharacter",
        classOf[Byte] -> "boxToByte",
        classOf[Short] -> "boxToShort",
        classOf[Int] -> "boxToInteger",
        classOf[Long] -> "boxToLong",
        classOf[Float] -> "boxToFloat",
        classOf[Double] -> "boxToDouble"
      )

      InvocationCountMatcher(
        classMatcher = ClassMatcher.ClassName(classOf[BoxesRunTime]),
        methodMatcher = MethodMatcher.Regex(
          new Regex(primitives.map(p =>
            s"(${primitiveToBoxedMethod(p)})"
          ).mkString("^", "|", "$")).pattern
        )
      )
    }

    def name: String = "Measurer.BoxingCount"
  }

  object BoxingCount {
    /** Creates BoxingCount measurer that counts boxing of all primitive values -
     *  boolean, char, byte, short, int, long, float and double.
     */
    def all() = new BoxingCount(classOf[Boolean], classOf[Char], classOf[Byte],
      classOf[Short], classOf[Int], classOf[Long], classOf[Float], classOf[Double])
  }

  /** Counts invocations of arbitrary method(s) specified by
   *  [[org.scalameter.execution.invocation.InvocationCountMatcher]].
   */
  case class MethodInvocationCount(matcher: InvocationCountMatcher) extends InvocationCount {
    def name: String = "Measurer.MethodInvocationCount"
  }

}


