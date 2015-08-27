package org.scalameter
package examples



import org.scalameter.reporting._
import org.scalameter.Key._



class TestSuite extends Bench.Group {
  performance of "memory" config(
    Key.reports.resultDir -> "target/benchmarks/memory"
  ) in {
    include(new MemoryTest2 {})
  }

  performance of "running time" config(
    Key.reports.resultDir -> "target/benchmarks/time"
  ) in {
    include(new RegressionTest3 {})
  }
}
