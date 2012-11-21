package org.scalameter



import collection._
import utils.Tree



abstract class PerformanceTest extends PerformanceTest.Initialization {

  def main(args: Array[String]) {
    val ctx = Main.Configuration.fromCommandLineArgs(args).context
    dyn.initialContext.withValue(ctx) {
      executeTests()
    }
  }

}


object PerformanceTest {

  trait Initialization extends DSL with DelayedInit {

    import DSL._

    def executor: org.scalameter.Executor

    def reporter: org.scalameter.Reporter

    def persistor: Persistor

    type SameType

    def executeTests() {
      val datestart = new java.util.Date
      DSL.setupzipper.value = Tree.Zipper.root[Setup[_]]
      testbody.value.apply()
      val setuptree = DSL.setupzipper.value.result
      val resulttree = executor.run(setuptree.asInstanceOf[Tree[Setup[SameType]]], reporter, persistor)
      val dateend = new java.util.Date

      val datedtree = resulttree.copy(context = resulttree.context + (Key.reports.startDate -> datestart) + (Key.reports.endDate -> dateend))
      reporter.report(datedtree, persistor)
    }

    def delayedInit(body: =>Unit) {
      testbody.value = () => body
    }

  }

  trait Quickbenchmark extends PerformanceTest {
    def executor = new execution.LocalExecutor(
      Executor.Warmer.Default(),
      Aggregator.min,
      new Executor.Measurer.Default
    )
    def reporter = new reporting.LoggingReporter
    def persistor = Persistor.None
  }

  trait Microbenchmark extends PerformanceTest {
    import Executor.Measurer
    def warmer = Executor.Warmer.Default()
    def aggregator = Aggregator.min
    def measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation {
      def frequency = 12
      def fullGC = true
    }
    def executor = execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    def reporter = new reporting.LoggingReporter
    def persistor = Persistor.None
  }

  trait Regression extends PerformanceTest {
    import Executor.Measurer
    import reporting._
    def warmer = Executor.Warmer.Default()
    def aggregator = Aggregator.complete(Aggregator.average)
    def measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation with Measurer.OutlierElimination with Measurer.RelativeNoise
    def executor = new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    def reporter = org.scalameter.Reporter.Composite(
      new RegressionReporter(RegressionReporter.Tester.ConfidenceIntervals(), RegressionReporter.Historian.ExponentialBackoff()),
      new HtmlReporter(HtmlReporter.Renderer.regression: _*)
    )
  }

}




















