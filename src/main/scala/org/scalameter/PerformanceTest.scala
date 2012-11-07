package org.scalameter



import collection._
import utils.Tree



abstract class PerformanceTest extends PerformanceTest.Initialization {

  def main(args: Array[String]) {
    Main.main(args :+ "-benches" :+ this.getClass.getName)
  }

}


object PerformanceTest {

  trait Initialization extends DSL with DelayedInit {

    import DSL._

    def executor: org.scalameter.Executor

    def reporter: org.scalameter.Reporter

    def persistor: Persistor
  
    protected def initSetupTree() {
      setupzipper.value = setupzipper.value.addContext(Key.dsl.executor -> executor.toString)
    }

    type SameType

    protected def executeTests() {
      val datestart = new java.util.Date
      val setuptree = setupzipper.value.result
      val resulttree = executor.run(setuptree.asInstanceOf[Tree[Setup[SameType]]], reporter, persistor)
      val dateend = new java.util.Date

      val datedtree = resulttree.copy(context = resulttree.context + (Key.reports.startDate -> datestart) + (Key.reports.endDate -> dateend))
      reporter.report(datedtree, persistor)
    }

    def delayedInit(body: =>Unit) {
      if (!DSL.withinInclude.value) {
        initSetupTree()
        body
        executeTests()
      } else body
    }

  }

  object Executor {
    import org.scalameter.Executor.Measurer

    trait BigOh extends PerformanceTest {
      lazy val aggregator = Aggregator.min
      lazy val measurer = new Measurer.Default()
      lazy val executor = execution.LocalExecutor(aggregator, measurer)
    }

    trait MinimalTime extends PerformanceTest {
      lazy val aggregator = Aggregator.min
      lazy val measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation {
        def frequency = 12
        def fullGC = true
      }
      lazy val executor = execution.JvmPerSetupExecutor(aggregator, measurer)
    }

    trait OptimalAllocation extends PerformanceTest {
      lazy val aggregator = Aggregator.median
      lazy val measurer = new Measurer.OptimalAllocation(new Measurer.IgnoringGC, aggregator)
      lazy val executor = new execution.JvmPerSetupExecutor(aggregator, measurer)
    }

    trait Regression extends PerformanceTest {
      lazy val aggregator = Aggregator.complete(Aggregator.average)
      lazy val measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation with Measurer.OutlierElimination with Measurer.RelativeNoise
      lazy val executor = new execution.MultipleJvmPerSetupExecutor(aggregator, measurer)
    }

  }

  object Reporter {
    import reporting._

    trait Logging extends PerformanceTest {
      lazy val reporter = new LoggingReporter
    }

    trait Chart extends PerformanceTest {
      lazy val reporter = new ChartReporter("", ChartReporter.ChartFactory.XYLine())
    }

    trait Html extends PerformanceTest {
      lazy val reporter = new HtmlReporter(HtmlReporter.Renderer.basic: _*)
    }

    trait Regression extends PerformanceTest {
      lazy val reporter = org.scalameter.Reporter.Composite(
        RegressionReporter(RegressionReporter.Tester.ConfidenceIntervals(0.02), RegressionReporter.Historian.ExponentialBackoff()),
        new HtmlReporter(HtmlReporter.Renderer.regression: _*)
      )
    }

  }

  trait Regression extends Executor.Regression with Reporter.Regression

}




















