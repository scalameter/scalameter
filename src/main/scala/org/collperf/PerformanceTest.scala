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

    trait MinTimeStatistic extends PerformanceTest {
      val executor = LocalExecutor(Aggregator.statistic(Aggregator.min))
    }

    trait NewJVM extends PerformanceTest {
      val executor = NewJVMExecutor
    }

  }

  object Reporter {

    trait Html extends PerformanceTest {
      val reporter = new reporters.HtmlReporter
    }

  }

}

