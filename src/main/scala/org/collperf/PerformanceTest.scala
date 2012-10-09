package org.collperf






trait PerformanceTest extends DSL {

  def executor: Executor

  def reporter: Reporter

}


object PerformanceTest {

  object Executor {

    trait MinTime extends PerformanceTest {
      val executor = execution.LocalExecutor(Aggregator.min)
    }

    trait NewJVM extends PerformanceTest {
      val executor = execution.NewJVMExecutor
    }

  }

  object Reporter {

    trait Console extends PerformanceTest {
      val reporter = new reporting.ConsoleReporter
    }

    trait Chart extends PerformanceTest {
      val reporter = new reporting.ChartReporter("", reporting.ChartReporter.ChartFactory.XYLine())
    }

    trait Html extends PerformanceTest {
      val reporter = new reporting.HtmlReporter(reporting.HtmlReporter.Renderer.all: _*)
    }

  }

}

