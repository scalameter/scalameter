package org



import java.util.Date
import collection._
import scala.util.DynamicVariable



package object scalameter {

  trait Foreach[T] {
    def foreach[U](f: T => U): Unit
  }

  class MonadicDynVar[T](v: T) extends DynamicVariable(v) {
    def using(nv: T) = new Foreach[Unit] {
      def foreach[U](f: Unit => U) = withValue(nv)(f())
    }
  }

  private[scalameter] object dyn {
    val initialContext = new MonadicDynVar(Context.topLevel)
    val log = new MonadicDynVar[Log](Log.Console)
    val events = new MonadicDynVar[Events](Events.None)
  }

  def initialContext: Context = dyn.initialContext.value

  def log: Log = dyn.log.value

  def events: Events = dyn.events.value

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

  /* events */

  case class Event(testName: String, description: String, result: Events.Result, throwable: Throwable)

  trait Events {
    def emit(e: Event): Unit
  }

  object Events {
    trait Result
    case object Success extends Result
    case object Failure extends Result
    case object Error extends Result
    case object Skipped extends Result

    case object None extends Events {
      def emit(e: Event) {}
    }
  }

  /* logging */

  trait Log {
    def error(msg: String): Unit
    def warn(msg: String): Unit
    def info(msg: String): Unit
    def debug(msg: String): Unit
    def trace(t: Throwable): Unit

    def verbose(msg: =>Any) = debug(msg.toString)
    def apply(msg: =>Any) = info(msg.toString)
  }

  object Log {

    case object None extends Log {
      def error(msg: String) {}
      def warn(msg: String) {}
      def info(msg: String) {}
      def debug(msg: String) {}
      def trace(t: Throwable) {}
    }

    case object Console extends Log {
      def error(msg: String) = info(msg)
      def warn(msg: String) = info(msg)
      def trace(t: Throwable) = info(t.getMessage)
      def info(msg: String) = log synchronized {
        println(msg)
      }
      def debug(msg: String) {
        if (initialContext.goe(Key.verbose, false)) log synchronized {
          println(msg)
        }
      }
    }

    case class Composite(logs: Log*) extends Log {
      def error(msg: String) = for (l <- logs) l.error(msg)
      def warn(msg: String) = for (l <- logs) l.warn(msg)
      def trace(t: Throwable) = for (l <- logs) l.trace(t)
      def info(msg: String) = for (l <- logs) l.info(msg)
      def debug(msg: String) = for (l <- logs) l.debug(msg)
    }

  }

  /* misc */

  def defaultClasspath = this.getClass.getClassLoader match {
    case urlcl: java.net.URLClassLoader => extractClasspath(urlcl)
    case cl => sys.props("java.class.path")
  }

  def extractClasspath(urlclassloader: java.net.URLClassLoader): String = {
    val fileResource = "file:(.*)".r
    val files = urlclassloader.getURLs.map(_.toString) collect {
      case fileResource(file) => file
    }
    files.mkString(":")
  }

}


package scalameter {

  import Key._

  case class Context(properties: immutable.Map[String, Any]) {
    def +(t: (String, Any)) = Context(properties + t)
    def ++(that: Context) = Context(this.properties ++ that.properties)
    def ++(that: Seq[(String, Any)]) = Context(this.properties ++ that)
    def get[T](key: String) = properties.get(key).asInstanceOf[Option[T]]
    def goe[T](key: String, v: T) = properties.getOrElse(key, v).asInstanceOf[T]

    def scope = properties(dsl.scope).asInstanceOf[List[String]].reverse.mkString(".")
    def curve = goe(dsl.curve, "")
  }

  object Context {
    def apply(xs: (String, Any)*) = new Context(immutable.Map(xs: _*))

    val empty = new Context(immutable.Map())

    val topLevel = machine ++ List(
      dsl.scope -> Nil,
      exec.benchRuns -> 36,
      exec.minWarmupRuns -> 10,
      exec.maxWarmupRuns -> 50,
      exec.jvmflags -> "-Xmx2048m -Xms2048m -XX:CompileThreshold=1",
      classpath -> defaultClasspath,
      reports.resultDir -> "tmp",
      reports.regression.significance -> 1e-10
    )

    def machine = Context(immutable.Map(
      Key.machine.jvm.version -> sys.props("java.vm.version"),
      Key.machine.jvm.vendor -> sys.props("java.vm.vendor"),
      Key.machine.jvm.name -> sys.props("java.vm.name"),
      Key.machine.osName -> sys.props("os.name"),
      Key.machine.osArch -> sys.props("os.arch"),
      Key.machine.cores -> Runtime.getRuntime.availableProcessors,
      Key.machine.hostname -> java.net.InetAddress.getLocalHost.getHostName
    ))
  }

  @SerialVersionUID(4203959258570851398L)
  case class Parameters(axisData: immutable.ListMap[String, Any]) {
    def ++(that: Parameters) = Parameters(this.axisData ++ that.axisData)
    def apply[T](key: String) = axisData.apply(key).asInstanceOf[T]

    override def toString = s"Parameters(${axisData.map(t => t._1 + " -> " + t._2).mkString(", ")})"
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
    def curveTable: Map[String, Seq[CurveData]] = results.map(_._3).flatten.groupBy(_.context.curve)

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

  /** Import the contents of this singleton object to obtain access to most abstractions
   *  in the ScalaMeter API.
   *
   *  Note that some definitions might shadow others, so if you import the contents of this
   *  object, you should not import the contents of other packages directly.
   *
   *  This object contains:
   *  - basic datatypes and singleton objects for writing tests, such as `PerformanceTest`
   *  - all the context map keys
   *  - contents of the `execution` package
   *  - contents of the `reporting` package
   *  - contents of the `persistance` package
   *  - the `Executor.Measurer` object
   *  - the `RegressionReporter.Tester` object
   *  - the `RegressionReporter.Historian` object
   *  - the `ChartReporter.ChartFactory` object
   *  - the `HtmlReporter.Renderer` object
   *  - and much more...
   */
  object api extends Key {

    type Gen[T] = org.scalameter.Gen[T]
    val Gen = org.scalameter.Gen

    type PerformanceTest = org.scalameter.PerformanceTest
    val PerformanceTest = org.scalameter.PerformanceTest

    type Reporter = org.scalameter.Reporter
    val Reporter = org.scalameter.Reporter

    type Persistor = org.scalameter.Persistor
    val Persistor = org.scalameter.Persistor

    /* execution */

    val LocalExecutor = execution.LocalExecutor
    val JvmPerSetupExecutor = execution.JvmPerSetupExecutor
    val MultipleJvmPerSetupExecutor = execution.MultipleJvmPerSetupExecutor

    val Measurer = Executor.Measurer

    /* reporting */

    type ChartReporter = reporting.ChartReporter
    val ChartReporter = reporting.ChartReporter

    type HtmlReporter = reporting.HtmlReporter
    val HtmlReporter = reporting.HtmlReporter

    type LogginReporter = reporting.LoggingReporter
    val LoggingReporter = reporting.LoggingReporter

    type RegressionReporter = reporting.RegressionReporter
    val RegressionReporter = reporting.RegressionReporter

    val Tester = reporting.RegressionReporter.Tester
    val Historian = reporting.RegressionReporter.Historian
    val ChartFactory = reporting.ChartReporter.ChartFactory
    val Renderer = reporting.HtmlReporter.Renderer

    /* persistance */

    type SerializationPersistor = persistance.SerializationPersistor
    val SerializationPersistor = persistance.SerializationPersistor

  }

}























