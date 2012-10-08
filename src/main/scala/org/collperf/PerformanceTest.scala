package org.collperf






trait PerformanceTest extends DSL {

  def executor: Executor

  def reporter: Reporter

}


object PerformanceTest {

  object Executor {

    trait MinTime extends PerformanceTest {
      val executor = LocalExecutor(Aggregator.min)
    }

    trait NewJVM extends PerformanceTest {
      val executor = NewJVMExecutor
    }

  }

  object Reporter {

    trait Console extends PerformanceTest {
      val reporter = new reporters.ConsoleReporter
    }

    trait Chart extends PerformanceTest {
      val reporter = new reporters.ChartReporter("", reporters.ChartReporter.ChartFactory.XYLine())
    }

    trait Html extends PerformanceTest {
      val reporter = new reporters.HtmlReporter(reporters.HtmlReporter.Renderer.all: _*)
    }

  }

}

