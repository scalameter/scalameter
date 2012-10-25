package org.collperf






trait PerformanceTest extends DSL {

  def executor: Executor

  def reporter: Reporter

  def persistor: Persistor

}


object PerformanceTest {

  object Executor {
    import org.collperf.Executor.Measurer

    trait BigOh extends PerformanceTest {
      lazy val aggregator = Aggregator.min
      lazy val measurer = new Measurer.Default()
      lazy val executor = execution.LocalExecutor(aggregator, measurer)
      lazy val persistor = Persistor.None
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
      lazy val measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation with Measurer.OutlierElimination {
        def frequency = 10
        def fullGC = false
      }
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
      lazy val reporter = new HtmlReporter(HtmlReporter.Renderer.all: _*)
    }

    trait Regression extends PerformanceTest {
      lazy val reporter = org.collperf.Reporter.Composite(
        RegressionReporter(RegressionReporter.Tester.ConfidenceIntervals(0.02), RegressionReporter.Historian.Default()),
        new HtmlReporter(HtmlReporter.Renderer.all: _*)
      )
    }

  }

  trait Regression extends Executor.Regression with Reporter.Regression

}




















