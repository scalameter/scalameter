package org.collperf






trait PerformanceTest extends DSL {

  def executor: Executor

  def reporter: Reporter

}


object PerformanceTest {

  object Executor {
    import org.collperf.Executor.Measurer

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
      lazy val aggregator = Aggregator.min
      lazy val measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation {
        def frequency = 20
        def fullGC = false
      }
      lazy val executor = new execution.JvmPerMeasurementExecutor(aggregator, measurer)
    }

  }

  object Reporter {

    trait Console extends PerformanceTest {
      lazy val reporter = new reporting.ConsoleReporter
    }

    trait Chart extends PerformanceTest {
      lazy val reporter = new reporting.ChartReporter("", reporting.ChartReporter.ChartFactory.XYLine())
    }

    trait Html extends PerformanceTest {
      lazy val reporter = new reporting.HtmlReporter(reporting.HtmlReporter.Renderer.all: _*)
    }

  }

}




















