package org

import language.implicitConversions
import language.postfixOps
import language.reflectiveCalls



import java.io.File
import java.net.URLClassLoader
import java.util.Date
import collection._
import scala.util.DynamicVariable



package object scalameter {

  trait Foreach[T] {
    def foreach[U](f: T => U): Unit
  }

  class MonadicDynVar[T](v: T) extends DynamicVariable(v) {
    def using(nv: T) = new Foreach[Unit] {
      def foreach[U](f: Unit => U): Unit = withValue(nv)(f())
      def map[S](f: Unit => S): S = withValue(nv)(f())
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

  implicit def fun2ops(f: Seq[Double] => Double) = new {
    def toAggregator(n: String) = {
      val function = f
      new Aggregator {
        def name = n
        def apply(times: Seq[Double]) = function(times)
        def data(times: Seq[Double]) = None
      }
    }
  }

  implicit final class SeqDoubleOps(val sq: Seq[Double]) extends AnyVal {
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

  def defaultClasspath = extractClasspath(this.getClass.getClassLoader, sys.props("java.class.path"))

  def extractClasspath(classLoader: ClassLoader, default: => String): String =
    classLoader match {
      case urlclassloader: java.net.URLClassLoader => extractClasspath(urlclassloader)
      case _ =>
        val parent = classLoader.getParent
        if (parent != null)
          extractClasspath(parent, default)
        else
          default
    }

  def extractClasspath(urlclassloader: java.net.URLClassLoader): String = {
    val fileResource = "file:(.*)".r
    val files = urlclassloader.getURLs.map(_.toString) collect {
      case fileResource(file) => file
    }
    files.mkString(File.pathSeparator)
  }

  def singletonInstance[C](module: Class[C]) = module.getField("MODULE$").get(null).asInstanceOf[PerformanceTest]

}


package scalameter {

  import Key._

  case class Context(properties: immutable.Map[String, Any]) {
    def +(t: (String, Any)) = Context(properties + t)
    def ++(that: Context) = Context(this.properties ++ that.properties)
    def ++(that: Seq[(String, Any)]) = Context(this.properties ++ that)
    def get[T](key: String) = properties.get(key).asInstanceOf[Option[T]]
    def goe[T](key: String, v: T) = properties.getOrElse(key, v).asInstanceOf[T]
    def apply[T](key: String) = properties.apply(key).asInstanceOf[T]

    def scope = properties(dsl.scope).asInstanceOf[List[String]].reverse.mkString(".")
    def scopeList = properties(dsl.scope).asInstanceOf[List[String]].reverse
    def curve = goe(dsl.curve, "")
  }

  object Context {
    def apply(xs: (String, Any)*) = new Context(immutable.Map(xs: _*))

    val empty = new Context(immutable.Map())

    val topLevel = machine ++ List(
      preJDK7 -> false,
      dsl.scope -> Nil,
      exec.benchRuns -> 36,
      exec.minWarmupRuns -> 10,
      exec.maxWarmupRuns -> 50,
      exec.jvmflags -> "-Xmx2048m -Xms2048m -XX:CompileThreshold=100",
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

  class Errors(measurement: Measurement) {
    import measurement._

    /** [[http://en.wikipedia.org/wiki/Average Average]] of the set of measurements */
    lazy val average = complete.sum / complete.length
    /** [[http://en.wikipedia.org/wiki/Standard_deviation Standard deviation]] of the set of measurements*/
    lazy val sdeviation = math.sqrt(variance)
    /** [[http://en.wikipedia.org/wiki/Variance Variance]] of the set of measurements */
    lazy val variance   = complete.map(_ - average).map(x => x * x).sum / (complete.length - 1)
  }

  @SerialVersionUID(-2541697615491239986L)
  case class Measurement(value: Double, params: Parameters, data: Option[Measurement.Data], units: String) {
    def complete: Seq[Double] = data.get.complete
    def success: Boolean = data.map(_.success).getOrElse(true)
    def errors: Errors = data match {
      case None    => throw new Exception("The complete data is not available. Please wrap your current aggregator in Aggregator.complete: `Aggregator.complete(Aggregator.average)")
      case Some(_) => new Errors(this)
    }
    def failed = this.copy(data = Some(data.get.copy(success = false)))
  }

  object Measurement {
    implicit val ordering = Ordering.by[Measurement, Parameters](_.params)

    case class Data(complete: Seq[Double], success: Boolean)
  }

  case class CurveData(measurements: Seq[Measurement], info: Map[String, Any], context: Context) {
    def success = measurements.forall(_.success)
  }

  object CurveData {
    def empty = CurveData(Seq(), Map(), initialContext)
  }

  @SerialVersionUID(-2666789428423525666L)
  case class History(results: Seq[History.Entry], infomap: Map[String, Any] = Map.empty) {
    def info[T](key: String, fallback: T) = infomap.getOrElse(key, fallback).asInstanceOf[T]
    def curves = results.map(_._3)
    def dates = results.map(_._1)

    override def toString = s"History(${results.mkString("\n")},\ninfo: $infomap)"
  }

  object History {
    type Entry = (Date, Context, CurveData)
  }

  case class Setup[T](
    context: Context,
    gen: Gen[T],
    setupbeforeall: Option[() => Unit],
    teardownafterall: Option[() => Unit],
    setup: Option[T => Any],
    teardown: Option[T => Any],
    customwarmup: Option[() => Any],
    snippet: T => Any,
    @transient customExecutor: Executor
  ) {
    def setupBeforeAll = if (setupbeforeall.isEmpty) { () => } else { () => setupbeforeall.get.apply() }
    def teardownAfterAll = if (teardownafterall.isEmpty) { () => } else { () => teardownafterall.get.apply() }
    def setupFor(v: T) = if (setup.isEmpty) { () => } else { () => setup.get(v) }
    def teardownFor(v: T) = if (teardown.isEmpty) { () => } else { () => teardown.get(v) }
    def setupFor() = if (setup.isEmpty) { v: T => } else { v: T => setup.get(v) }
    def teardownFor() = if (teardown.isEmpty) { v: T => } else { v: T => teardown.get(v) }
    def regenerateFor(params: Parameters): () => T = () => gen.generate(params)
  }

  trait Aggregator extends (Seq[Double] => Double) with Serializable {
    def name: String
    def apply(times: Seq[Double]): Double
    def data(times: Seq[Double]): Option[Measurement.Data]
  }

  object Aggregator {

    case class Statistic(min: Double, max: Double, average: Double, stdev: Double, median: Double)

    def min = {
      xs: Seq[Double] => xs.min.toDouble
    } toAggregator "min"

    def max = {
      xs: Seq[Double] => xs.max.toDouble
    } toAggregator "max"

    def median = {
      xs: Seq[Double] =>
      val sorted = xs.sorted
      sorted(sorted.size / 2).toDouble
    } toAggregator "median"

    def average = { xs: Seq[Double] => xs.sum.toDouble / xs.size } toAggregator "average"

    def stdev = { xs: Seq[Double] => xs.stdev } toAggregator "stdev"

    def complete(a: Aggregator) = new Aggregator {
      def name = s"complete(${a.name})"
      def apply(times: Seq[Double]) = a(times)
      def data(times: Seq[Double]) = Some(Measurement.Data(times, true))
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
   *  - contents of the `persistence` package
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

    type Executor = org.scalameter.Executor
    val Executor = org.scalameter.Executor

    type Reporter = org.scalameter.Reporter
    val Reporter = org.scalameter.Reporter

    type Persistor = org.scalameter.Persistor
    val Persistor = org.scalameter.Persistor

    /* execution */

    val LocalExecutor = execution.LocalExecutor
    val SeparateJvmsExecutor = execution.SeparateJvmsExecutor

    val Aggregator = org.scalameter.Aggregator
    val Measurer = Executor.Measurer
    val Warmer = Executor.Warmer

    /* reporting */

    type ChartReporter = reporting.ChartReporter
    val ChartReporter = reporting.ChartReporter

    type HtmlReporter = reporting.HtmlReporter
    val HtmlReporter = reporting.HtmlReporter

    type LoggingReporter = reporting.LoggingReporter
    val LoggingReporter = reporting.LoggingReporter

    type RegressionReporter = reporting.RegressionReporter
    val RegressionReporter = reporting.RegressionReporter

    type DsvReporter = reporting.DsvReporter
    val DsvReporter = reporting.DsvReporter

    val Tester = reporting.RegressionReporter.Tester
    val Historian = reporting.RegressionReporter.Historian
    val ChartFactory = reporting.ChartReporter.ChartFactory

    /* persistence */

    type SerializationPersistor = persistence.SerializationPersistor
    val SerializationPersistor = persistence.SerializationPersistor

  }

}























