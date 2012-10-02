package org.collperf






trait PerformanceTest extends DSL with HasExecutor {

}


object PerformanceTest {

  trait LeastTime extends PerformanceTest {
    val executor = LocalExecutor.min
  }

  trait NewJVM extends PerformanceTest {
    val executor = NewJVMExecutor
  }

}

