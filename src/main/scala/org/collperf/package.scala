package org



import java.util.Date
import collection._
import scala.util.DynamicVariable



package object collperf {

  @volatile var initialContext = Context.topLevel

  /* decorators */

  implicit def fun2ops(f: Seq[Long] => Double) = new {
    def toAggregator(n: String) = {
      val function = f
      new Aggregator {
        def name = n
        def apply(times: Seq[Long]) = function(times)
        def data(times: Seq[Long]) = None
      }
    }
  }

  implicit final class SeqLongOps(val sq: Seq[Long]) extends AnyVal {
    def stdev: Double = {
      val m = 1.0 * sq.sum / sq.size
      var s = 0.0
      for (v <- sq) {
        val diff = v - m
        s += diff * diff
      }
      math.sqrt(s / (sq.size - 1))
    }
  }

  implicit final class SeqOps[T](val sq: Seq[T]) extends AnyVal {
    def orderedGroupBy[K](f: T => K): Map[K, Seq[T]] = {
      val map = mutable.LinkedHashMap[K, mutable.ArrayBuffer[T]]()

      for (elem <- sq) {
        val key = f(elem)
        map.get(key) match {
          case Some(b) => b += elem
          case None => map(key) = mutable.ArrayBuffer(elem)
        }
      }

      map
    }
  }

  /* logging */

  object log {
    def apply(msg: =>Any) = log synchronized {
      println(msg)
    }

    def verbose(msg: =>Any) {
      if (initialContext.goe(Key.verbose, false)) log synchronized {
        println(msg)
      }
    }
  }

}


package collperf {

  object Key {
    val curve = "curve"
    val scope = "scope"
    val executor = "executor"

    val jvmVersion = "jvm-version"
    val jvmVendor = "jvm-vendor"
    val jvmName = "jvm-name"
    val osName = "os-name"
    val osArch = "os-arch"
    val cores = "cores"
    val hostname = "hostname"

    val startDate = "date-start"
    val endDate = "date-end"

    val benchRuns = "runs"
    val minWarmupRuns = "min-warmups"
    val maxWarmupRuns = "max-warmups"
    val verbose = "verbose"
    val resultDir = "result-dir"
    val significance = "significance"
    val independentSamples = "independent-samples"
    val frequency = "frequency"
    val fullGC = "full-gc"
    val suspectPercent = "suspect-percent"
    val covMultiplier = "cov-multiplier"
    val noiseMagnitude = "noise-magnitude"
    val retries = "retries"

    val timeIndices = "time-indices"

    val bigO = "big-o"

    val unit = "unit"
  }

  case class Context(properties: immutable.Map[String, Any]) {
    def +(t: (String, Any)) = Context(properties + t)
    def ++(that: Context) = Context(this.properties ++ that.properties)
    def get[T](key: String) = properties.get(key).asInstanceOf[Option[T]]
    def goe[T](key: String, v: T) = properties.getOrElse(key, v).asInstanceOf[T]

    def scope = properties(Key.scope).asInstanceOf[List[String]].reverse.mkString(".")
    def curve = goe(Key.curve, "")
  }

  object Context {
    def apply(xs: (String, Any)*) = new Context(immutable.Map(xs: _*))

    val empty = new Context(immutable.Map())

    val topLevel = machine + (Key.scope -> Nil) + (Key.benchRuns -> 36) + (Key.minWarmupRuns -> 10) + (Key.maxWarmupRuns -> 50)

    def machine = Context(immutable.Map(
      Key.jvmVersion -> sys.props("java.vm.version"),
      Key.jvmVendor -> sys.props("java.vm.vendor"),
      Key.jvmName -> sys.props("java.vm.name"),
      Key.osName -> sys.props("os.name"),
      Key.osArch -> sys.props("os.arch"),
      Key.cores -> Runtime.getRuntime.availableProcessors,
      Key.hostname -> java.net.InetAddress.getLocalHost.getHostName
    ))
  }

  @SerialVersionUID(4203959258570851398L)
  case class Parameters(axisData: immutable.ListMap[String, Any]) {
    def ++(that: Parameters) = Parameters(this.axisData ++ that.axisData)
    def apply[T](key: String) = axisData.apply(key).asInstanceOf[T]
  }

  object Parameters {
    def apply(xs: (String, Any)*) = new Parameters(immutable.ListMap(xs: _*))

    implicit val ordering = Ordering.by[Parameters, Iterable[String]] {
      _.axisData.toSeq.map(_._1).sorted.toIterable
    }
  }

  @SerialVersionUID(-2541697615491239986L)
  case class Measurement(time: Double, params: Parameters, data: Option[Any]) {
    def complete: Seq[Long] = data.get.asInstanceOf[Seq[Long]]
  }

  object Measurement {
    implicit val ordering = Ordering.by[Measurement, Parameters](_.params)
  }

  case class CurveData(measurements: Seq[Measurement], info: Map[String, Any], context: Context)

  @SerialVersionUID(-2666789428423525666L)
  case class History(results: Seq[History.Entry], infomap: Map[String, Any] = Map.empty) {
    def info[T](key: String, fallback: T) = infomap.getOrElse(key, fallback).asInstanceOf[T]

    override def toString = s"History(${results.mkString("\n")},\ninfo: $infomap)"
  }

  object History {
    type Entry = (Date, Context, Seq[CurveData])
  }

  case class Setup[T](context: Context, gen: Gen[T], setup: Option[T => Any], teardown: Option[T => Any], customwarmup: Option[() => Any], snippet: T => Any) {
    def setupFor(v: T) = if (setup.isEmpty) { () => } else { () => setup.get(v) }
    def teardownFor(v: T) = if (teardown.isEmpty) { () => } else { () => teardown.get(v) }
    def setupFor() = if (setup.isEmpty) { v: T => } else { v: T => setup.get(v) }
    def teardownFor() = if (teardown.isEmpty) { v: T => } else { v: T => teardown.get(v) }
    def regenerateFor(params: Parameters): () => T = () => gen.generate(params)
  }

  trait Aggregator extends (Seq[Long] => Double) with Serializable {
    def name: String
    def apply(times: Seq[Long]): Double
    def data(times: Seq[Long]): Option[Any]
  }

  object Aggregator {
    
    case class Statistic(min: Double, max: Double, average: Double, stdev: Double, median: Double)

    def min = {
      xs: Seq[Long] => xs.min.toDouble
    } toAggregator "min"

    def max = {
      xs: Seq[Long] => xs.max.toDouble
    } toAggregator "max"

    def median = {
      xs: Seq[Long] =>
      val sorted = xs.sorted
      sorted(sorted.size / 2).toDouble
    } toAggregator "median"

    def average = { xs: Seq[Long] => xs.sum.toDouble / xs.size } toAggregator "average"

    def stdev = { xs: Seq[Long] => xs.stdev } toAggregator "stdev"

    def statistic(a: Aggregator) = new Aggregator {
      def name = a.name
      def apply(times: Seq[Long]) = a(times)
      def data(times: Seq[Long]) = Some(Statistic(
        min = Aggregator.min(times),
        max = Aggregator.max(times),
        average = Aggregator.average(times),
        stdev = Aggregator.stdev(times),
        median = Aggregator.median(times)
      ))
    }

    def complete(a: Aggregator) = new Aggregator {
      def name = s"complete(${a.name})"
      def apply(times: Seq[Long]) = a(times)
      def data(times: Seq[Long]) = Some(times)
    }

    def withData(a: Aggregator)(as: Aggregator*) = new Aggregator {
      def name = a.name
      def apply(times: Seq[Long]) = a(times)
      def data(times: Seq[Long]) = Some((for (a <- as) yield {
        (a.name, a.apply(times))
      }).toMap)
    }
  }

}























