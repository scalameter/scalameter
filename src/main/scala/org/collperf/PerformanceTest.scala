package org.collperf






trait PerformanceTest extends DSL {

  def executor: Executor

  def reporter: Reporter

}


object PerformanceTest {

  object Executor {

    trait LocalMin extends PerformanceTest {
      lazy val executor = execution.LocalExecutor(Aggregator.min, new org.collperf.Executor.Measurer.Default())
    }

    trait NewJvmMedian extends PerformanceTest {
      lazy val executor = execution.NewJvmExecutor(Aggregator.average, new org.collperf.Executor.Measurer.Default())
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

