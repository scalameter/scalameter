package org.scalameter



import org.scalameter.picklers.Implicits._



abstract class Bench[U] extends DSL[U] with Serializable {

  def main(args: Array[String]) {
    val ctx = dyn.currentContext.value ++ Main.Configuration.fromCommandLineArgs(args).context
    val ok = withTestContext(ctx, Log.Console, Events.None) {
      executeTests()
    }

    if (!ok) sys.exit(1)
  }

}


object Bench {

  @deprecated("Please use Bench.Quick instead", "0.7")
  type Quickbenchmark = Quick
  
  /** Quick benchmark run in the same JVM.
   *  Reports result into the console.
   */
  abstract class Quick extends Bench[Double] {
    def executor: Executor[Double] = new execution.LocalExecutor(
      Warmer.Default(),
      Aggregator.min,
      measurer
    )
    def measurer: Measurer[Double] = new Measurer.Default
    def reporter: Reporter[Double] = new reporting.LoggingReporter
    def persistor: Persistor = Persistor.None
  }

  @deprecated("Please use Bench.Micro instead", "0.7")
  type Microbenchmark = Micro
  
  /** A more reliable benchmark run in a separate JVM.
   *  Reports results into the console.
   */
  abstract class Micro extends Bench[Double] {
    def warmer: Warmer = Warmer.Default()
    def aggregator: Aggregator[Double] = Aggregator.min
    def measurer: Measurer[Double] = new Measurer.IgnoringGC
      with Measurer.PeriodicReinstantiation[Double] {
      override val defaultFrequency = 12
      override val defaultFullGC = true
    }
    def executor: Executor[Double] =
      new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    def reporter: Reporter[Double] = new reporting.LoggingReporter
    def persistor: Persistor = Persistor.None
  }

  /** A base for benchmarks generating a more detailed (regression) report, potentially online.
   */
  abstract class HTMLReport extends Bench[Double] {
    import reporting._
    def persistor: Persistor = new persistence.GZIPJSONSerializationPersistor
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
    def tester: RegressionReporter.Tester
    def historian: RegressionReporter.Historian
    def online: Boolean
    def reporter: Reporter[Double] = Reporter.Composite(
      new RegressionReporter(tester, historian),
      HtmlReporter(!online)
    )
  }


  /** Runs in separate JVM, performs regression tests and prepares HTML document for online hosting.
   */
  abstract class OnlineRegressionReport extends HTMLReport {
    import reporting._
    def tester: RegressionReporter.Tester = RegressionReporter.Tester.OverlapIntervals()
    def historian: RegressionReporter.Historian = RegressionReporter.Historian.ExponentialBackoff()
    def online = true
  }

  /** Runs in separate JVM, performs regression tests and prepares an offline HTML document.
   */
  abstract class OfflineRegressionReport extends HTMLReport {
    import reporting._
    def tester: RegressionReporter.Tester = RegressionReporter.Tester.OverlapIntervals()
    def historian: RegressionReporter.Historian = RegressionReporter.Historian.ExponentialBackoff()
    def online = false
  }

  /** Runs in separate JVM and prepares an offline HTML document.
   *  Does not regression testing.
   */
  abstract class OfflineReport extends HTMLReport {
    import reporting._
    def tester: RegressionReporter.Tester = RegressionReporter.Tester.Accepter()
    def historian: RegressionReporter.Historian = RegressionReporter.Historian.ExponentialBackoff()
    def online = false
  }

  @deprecated("This performance test is now deprecated, please use `OnlineRegressionReport` instead.", "0.5")
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
    def executor: Executor[Double] = new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    def reporter: Reporter[Double] = Reporter.Composite(
      new RegressionReporter(RegressionReporter.Tester.OverlapIntervals(), RegressionReporter.Historian.ExponentialBackoff()),
      HtmlReporter(false)
    )
  }

}


















