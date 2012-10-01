package org.collperf






trait PerformanceTest extends DSL with HasExecutor {

}


object PerformanceTest {

  trait Default extends PerformanceTest {
    def executor = LocalExecutor.min
  }

}

