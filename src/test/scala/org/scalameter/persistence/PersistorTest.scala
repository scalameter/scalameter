package org.scalameter.persistence

import org.scalameter._
import org.scalatest.Matchers


trait PersistorTest { this: Matchers =>
  def afterBenchmark(p: org.scalameter.Persistor)(fun: InterceptingPersistor => Unit) = {
    val persistor = new InterceptingPersistor(p)
    val benchmark = new RangeBenchmark(persistor)
    withTestContext(currentContext, Log.Console, Events.None) {
      benchmark.executeTests()
    }
    fun(persistor)
  }

  def compareHistory(actual: History, expected: History): Unit = {
    actual.results.size should === (expected.results.size)
    actual.results.zip(expected.results).foreach { case (actualR, expectedR) =>
      actualR._1 should equal (expectedR._1)
      actualR._2.properties  should contain theSameElementsAs expectedR._2.properties
      actualR._3.context.properties should contain theSameElementsAs expectedR._3.context.properties
      actualR._3.info should contain theSameElementsAs expectedR._3.info
      actualR._3.measurements.size should === (expectedR._3.measurements.size)
      actualR._3.measurements.zip(expectedR._3.measurements).foreach { case (actualM, expectedM) =>
        actualM.params.axisData should contain theSameElementsAs expectedM.params.axisData
        actualM.units should === (expectedM.units)
        actualM.value should === (expectedM.value)
        actualM.data.success should === (expectedM.data.success)
        actualM.data.complete should contain theSameElementsInOrderAs expectedM.data.complete
      }
    }
    actual.infomap should contain theSameElementsAs expected.infomap
  }
}
