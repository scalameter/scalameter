package org.scalameter


import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.Pickler


abstract class Bench[U] extends DSL[U] with Serializable {

  def main(args: Array[String]) {
    val ctx = dyn.currentContext.value ++
      Main.Configuration.fromCommandLineArgs(args).context
    val ok = withTestContext(ctx, Log.Console, Events.None) {
      executeTests()
    }

    if (!ok) sys.exit(1)
  }

}


object Bench {

  class Group extends Bench[Nothing] with GroupedPerformanceTest {
    def measurer = Measurer.None
    def executor = Executor.None
    def persistor = Persistor.None
    def reporter = Reporter.None
  }

  /** Benchmark that runs snippets in the same JVM.
   *  Reports result into the console.
   */
  abstract class Local[U: Pickler] extends Bench[U] {
    def warmer: Warmer = new Warmer.Default

    def aggregator: Aggregator[U]

    def executor: Executor[U] = new execution.LocalExecutor(
      warmer,
      aggregator,
      measurer
    )

    def persistor: Persistor = Persistor.None

    def reporter: Reporter[U] = new reporting.LoggingReporter
  }

  /** Benchmark that runs snippets in separate JVMs.
   *  Reports result into the console.
   */
  abstract class Forked[U: Pickler: PrettyPrinter] extends Bench[U] {
    def warmer: Warmer = new Warmer.Default

    def aggregator: Aggregator[U]

    def executor: Executor[U] = new execution.SeparateJvmsExecutor(
      warmer,
      aggregator,
      measurer
    )

    def persistor: Persistor = Persistor.None

    def reporter: Reporter[U] = new reporting.LoggingReporter
  }

  /** Benchmark that runs snippets in separate JVMs.
   *  It persists results in the gzipped JSON format with appropriate reporter.
   */
  abstract class Persisted[U: Pickler: PrettyPrinter] extends Bench[U] {
    def warmer: Warmer = new Warmer.Default

    def aggregator: Aggregator[U]

    def executor: Executor[U] = new execution.SeparateJvmsExecutor[U](
      warmer,
      aggregator,
      measurer
    )

    def persistor: Persistor = new persistence.GZIPJSONSerializationPersistor
  }

  @deprecated("Please use Bench.LocalTime instead", "0.7")
  type Quickbenchmark = LocalTime
  
  /** Quick benchmark runs snippets in the same JVM.
   *  Reports execution time into the console.
   */
  abstract class LocalTime extends Local[Double] {
    def measurer: Measurer[Double] = new Measurer.Default
    def aggregator: Aggregator[Double] = Aggregator.min
  }

  @deprecated("Please use Bench.ForkedTime instead", "0.7")
  type Microbenchmark = ForkedTime
  
  /** A more reliable benchmark run in a separate JVM.
   *  Reports execution time into the console.
   */
  abstract class ForkedTime extends Forked[Double] {
    def aggregator: Aggregator[Double] = Aggregator.min
    def measurer: Measurer[Double] = new Measurer.IgnoringGC
      with Measurer.PeriodicReinstantiation[Double] {
      override val defaultFrequency = 12
      override val defaultFullGC = true
    }
  }

  /** A base for benchmarks generating a more detailed (regression) report,
   *  potentially online.
   */
  abstract class HTMLReport extends Persisted[Double] {
    import reporting._
    def aggregator: Aggregator[Double] = Aggregator.average
    def measurer: Measurer[Double] = new Measurer.IgnoringGC
      with Measurer.PeriodicReinstantiation[Double]
      with Measurer.OutlierElimination[Double]
      with Measurer.RelativeNoise {
      def numeric: Numeric[Double] = implicitly[Numeric[Double]]
    }
    def tester: RegressionReporter.Tester
    def historian: RegressionReporter.Historian
    def online: Boolean
    def reporter: Reporter[Double] = Reporter.Composite(
      new RegressionReporter(tester, historian),
      HtmlReporter(!online)
    )
  }

  /** Runs in separate JVM, performs regression tests and
   *  prepares HTML document for online hosting.
   */
  abstract class OnlineRegressionReport extends HTMLReport {
    import reporting._
    def tester: RegressionReporter.Tester =
      RegressionReporter.Tester.OverlapIntervals()
    def historian: RegressionReporter.Historian =
      RegressionReporter.Historian.ExponentialBackoff()
    def online = true
  }

  /** Runs in separate JVM, performs regression tests and
   *  prepares an offline HTML document.
   */
  abstract class OfflineRegressionReport extends HTMLReport {
    import reporting._
    def tester: RegressionReporter.Tester =
      RegressionReporter.Tester.OverlapIntervals()
    def historian: RegressionReporter.Historian =
      RegressionReporter.Historian.ExponentialBackoff()
    def online = false
  }

  /** Runs in separate JVM and prepares an offline HTML document.
   *  Does not regression testing.
   */
  abstract class OfflineReport extends HTMLReport {
    import reporting._
    def tester: RegressionReporter.Tester =
      RegressionReporter.Tester.Accepter()
    def historian: RegressionReporter.Historian =
      RegressionReporter.Historian.ExponentialBackoff()
    def online = false
  }

  @deprecated(
    "This performance test is now deprecated, " +
    "please use `OnlineRegressionReport` instead.",
    "0.5")
  abstract class Regression extends Bench[Double] {
    import reporting._
    def warmer: Warmer = Warmer.Default()
    def aggregator: Aggregator[Double] = Aggregator.average
    def measurer: Measurer[Double] = new Measurer.IgnoringGC
      with Measurer.PeriodicReinstantiation[Double]
      with Measurer.OutlierElimination[Double]
      with Measurer.RelativeNoise {
      def numeric: Numeric[Double] = implicitly[Numeric[Double]]
    }
    def executor: Executor[Double] =
      new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    def reporter: Reporter[Double] = Reporter.Composite(
      new RegressionReporter(
        RegressionReporter.Tester.OverlapIntervals(),
        RegressionReporter.Historian.ExponentialBackoff()),
      HtmlReporter(false)
    )
  }

}
