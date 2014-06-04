package org.scalameter

import java.util.Date
import scala.collection._
import utils.Tree

abstract class PerformanceTest extends PerformanceTest.Initialization with Serializable {

  def main(args: Array[String]) {
    val ctx = Main.Configuration.fromCommandLineArgs(args).context
    val ok = dyn.currentContext.withValue(ctx) {
      executeTests()
    }

    if (!ok) sys.exit(1)
  }

}


object PerformanceTest {

  trait Initialization extends DSL {

    import DSL._

    def executor: org.scalameter.Executor

    def reporter: org.scalameter.Reporter

    def persistor: Persistor

    def defaultConfig: Context = Context.empty

    type SameType

    def executeTests(): Boolean = {
      val datestart: Option[Date] = Some(new Date)
      val rawsetuptree = DSL.setupzipper.value.result
      val setuptree = rawsetuptree.filter(setupFilter)
      val resulttree = executor.run(setuptree.asInstanceOf[Tree[Setup[SameType]]], reporter, persistor)
      val dateend: Option[Date] = Some(new Date)

      val datedtree = resulttree.copy(context = resulttree.context + (Key.reports.startDate -> datestart) + (Key.reports.endDate -> dateend))
      reporter.report(datedtree, persistor)
    }

  }

  private def setupFilter(setup: Setup[_]): Boolean = {
    val sf = currentContext(Key.scopeFilter)
    val fullname = setup.context.scope + "." + setup.context.curve
    val regex = sf.r
    regex.findFirstIn(fullname) != None
  }
  
  /** Quick benchmark run in the same JVM.
   *  Reports result into the console.
   */
  trait Quickbenchmark extends PerformanceTest {
    def executor = new execution.LocalExecutor(
      Warmer.Default(),
      Aggregator.min,
      measurer
    )
    def measurer = new Measurer.Default
    def reporter = new reporting.LoggingReporter
    def persistor = Persistor.None
  }

  /** A more reliable benchmark run in a separate JVM.
   *  Reports results into the console.
   */
  trait Microbenchmark extends PerformanceTest {
    def warmer = Warmer.Default()
    def aggregator = Aggregator.min
    def measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation {
      override val defaultFrequency = 12
      override val defaultFullGC = true
    }
    def executor = execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    def reporter = new reporting.LoggingReporter
    def persistor = Persistor.None
  }

  /** A base for benchmarks generating a more detailed (regression) report, potentially online.
   */
  trait HTMLReport extends PerformanceTest {
    import reporting._
    def persistor = new persistence.SerializationPersistor
    def warmer = Warmer.Default()
    def aggregator = Aggregator.average
    def measurer: Measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation with Measurer.OutlierElimination with Measurer.RelativeNoise
    def executor: Executor = new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    def tester: RegressionReporter.Tester
    def historian: RegressionReporter.Historian
    def online: Boolean
    def reporter: Reporter = Reporter.Composite(
      new RegressionReporter(tester, historian),
      HtmlReporter(!online)
    )
  }


  /** Runs in separate JVM, performs regression tests and prepares HTML document for online hosting.
   */
  trait OnlineRegressionReport extends HTMLReport {
    import reporting._
    def tester: RegressionReporter.Tester = RegressionReporter.Tester.OverlapIntervals()
    def historian: RegressionReporter.Historian = RegressionReporter.Historian.ExponentialBackoff()
    def online = true
  }

  /** Runs in separate JVM, performs regression tests and prepares an offline HTML document.
   */
  trait OfflineRegressionReport extends HTMLReport {
    import reporting._
    def tester: RegressionReporter.Tester = RegressionReporter.Tester.OverlapIntervals()
    def historian: RegressionReporter.Historian = RegressionReporter.Historian.ExponentialBackoff()
    def online = false
  }

  /** Runs in separate JVM and prepares an offline HTML document.
   *  Does not regression testing.
   */
  trait OfflineReport extends HTMLReport {
    import reporting._
    def tester: RegressionReporter.Tester = RegressionReporter.Tester.Accepter()
    def historian: RegressionReporter.Historian = RegressionReporter.Historian.ExponentialBackoff()
    def online = false
  }

  @deprecated("This performance test is now deprecated, please use `OnlineRegressionReport` instead.", "0.5")
  trait Regression extends PerformanceTest {
    import reporting._
    def warmer = Warmer.Default()
    def aggregator = Aggregator.average
    def measurer: Measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation with Measurer.OutlierElimination with Measurer.RelativeNoise
    def executor: Executor = new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    def reporter: Reporter = Reporter.Composite(
      new RegressionReporter(RegressionReporter.Tester.OverlapIntervals(), RegressionReporter.Historian.ExponentialBackoff()),
      HtmlReporter(false)
    )
  }

}


















