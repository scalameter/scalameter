package org.scalameter
package examples



import reporting._
import Key._



class MemoryTest extends PerformanceTest.Regression {

  def persistor = new persistence.SerializationPersistor
  override def measurer = new Executor.Measurer.MemoryFootprint

  val sizes = Gen.range("size")(1000000, 5000000, 2000000)

  performance of "MemoryFootprint" in {
    performance of "Array" in {
      using(sizes) config (
        exec.benchRuns -> 10,
        exec.independentSamples -> 2
      ) in { sz =>
        (0 until sz).toArray
      }
    }
  }

}















