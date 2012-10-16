package org



import java.util.Date
import collection._
import scala.util.DynamicVariable



package object collperf {

  @volatile var initialContext = Context.topLevel

  /* decorators */

  implicit def fun2ops(f: Seq[Long] => Long) = new {
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

    val benchRuns = "runs"
    val warmupRuns = "warmups"
    val verbose = "verbose"
    val resultDir = "result-dir"

    val persistor = "persistor"
    val bigO = "big-o"
  }

  case class Context(properties: immutable.Map[String, Any]) {
    def +(t: (String, Any)) = Context(properties + t)
    def ++(that: Context) = Context(this.properties ++ that.properties)
    def get[T](key: String) = properties.get(key).asInstanceOf[Option[T]]
    def goe[T](key: String, v: T) = properties.getOrElse(key, v).asInstanceOf[T]

    def scope = properties(Key.scope).asInstanceOf[List[String]].reverse.mkString(".")
  }

  object Context {
    def apply(xs: (String, Any)*) = new Context(immutable.Map(xs: _*))

    val empty = new Context(immutable.Map())

    val topLevel = machine + (Key.scope -> Nil)

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

  case class Parameters(axisData: immutable.ListMap[String, Any]) {
    def ++(that: Parameters) = Parameters(this.axisData ++ that.axisData)
    def apply[T](key: String) = axisData.apply(key).asInstanceOf[T]
  }

  object Parameters {
    def apply(xs: (String, Any)*) = new Parameters(immutable.ListMap(xs: _*))
  }

  case class Measurement(time: Long, params: Parameters, data: Option[Any])

  case class CurveData(measurements: Seq[Measurement], info: Map[String, Any], context: Context)

  case class History(results: Seq[(Date, Context, Seq[CurveData])])

  case class Setup[T](context: Context, gen: Gen[T], setup: Option[T => Any], teardown: Option[T => Any], customwarmup: Option[() => Any], snippet: T => Any) {
    def setupFor(v: T) = if (setup.isEmpty) { () => } else { () => setup.get(v) }
    def teardownFor(v: T) = if (teardown.isEmpty) { () => } else { () => teardown.get(v) }
    def setupFor() = if (setup.isEmpty) { v: T => } else { v: T => setup.get(v) }
    def teardownFor() = if (teardown.isEmpty) { v: T => } else { v: T => teardown.get(v) }
  }

  trait Aggregator extends (Seq[Long] => Long) with Serializable {
    def name: String
    def apply(times: Seq[Long]): Long
    def data(times: Seq[Long]): Option[Any]
  }

  object Aggregator {
    
    case class Statistic(min: Long, max: Long, average: Long, stdev: Long, median: Long)

    def min = {
      xs: Seq[Long] => xs.min
    } toAggregator "min"

    def max = {
      xs: Seq[Long] => xs.max
    } toAggregator "max"

    def median = {
      xs: Seq[Long] =>
      val sorted = xs.sorted
      sorted(sorted.size / 2)
    } toAggregator "median"

    def average = { xs: Seq[Long] => xs.sum / xs.size } toAggregator "average"

    def stdev = { xs: Seq[Long] => xs.stdev.toLong } toAggregator "stdev"

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
      def name = a.name
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























