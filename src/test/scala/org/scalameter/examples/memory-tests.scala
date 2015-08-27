package org.scalameter
package examples



import reporting._
import Key._



class MemoryTest extends Bench.OfflineReport {
  override def measurer = new Measurer.MemoryFootprint

  val sizes = Gen.range("size")(1000000, 5000000, 2000000)

  performance of "MemoryFootprint" in {
    performance of "Array" in {
      using(sizes) config (
        exec.minWarmupRuns -> 2,
        exec.maxWarmupRuns -> 5,
        exec.benchRuns -> 5,
        exec.independentSamples -> 1
      ) in { sz =>
        (0 until sz).toArray
      }
    }
  }

}


trait MemoryTest2 extends Bench.OfflineReport {
  override def measurer = new Measurer.MemoryFootprint

  val sizes = Gen.range("size")(1000000, 5000000, 2000000)

  performance of "MemoryFootprint" in {
    performance of "Array" in {
      using(sizes) config (
        exec.minWarmupRuns -> 2,
        exec.maxWarmupRuns -> 5,
        exec.benchRuns -> 30,
        exec.independentSamples -> 1
      ) in { sz =>
        (0 until sz).toArray
      }
    }
  }

}
