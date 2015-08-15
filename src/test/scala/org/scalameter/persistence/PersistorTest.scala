package org.scalameter.persistence

import java.io.File
import org.scalameter._
import org.scalatest._


trait PersistorTest { this: Matchers =>
  def persistor: IOStreamPersistor[_, _]

  def executeBenchmark()(fun: InterceptingPersistor => Any) = {
    val p = new InterceptingPersistor(persistor)
    val benchmark = new RangeBenchmark(p)
    try {
      withTestContext(currentContext, Log.Console, Events.None) {
        benchmark.executeTests()
      }
      fun(p)
    } finally {
      p.intercepted.foreach { case (ctx, _) =>
        p.fileFor(ctx).delete()
      }
    }
  }

  /** Checks if size of file is less than value given in kB.
   */
  def compareSpaceConsumption(location: File, maxValue: Double): Unit = {
    val bytes = utils.IO.readFromFile(location)
    val total = bytes.length / 1024d
    total should be < maxValue
  }

  def compareHistory(actual: History[_], expected: History[_]): Unit = {
    actual.results.size should === (expected.results.size)
    actual.results.zip(expected.results).foreach { case (actualR, expectedR) =>
      actualR._1 should equal (expectedR._1)
      actualR._2.properties should contain theSameElementsAs expectedR._2.properties.filterNot(_._1.isTransient)
      actualR._3.context.properties should contain theSameElementsAs expectedR._3.context.properties.filterNot(_._1.isTransient)
      actualR._3.info should contain theSameElementsAs expectedR._3.info.filterNot(_._1.isTransient)
      actualR._3.measurements.size should === (expectedR._3.measurements.size)
      actualR._3.measurements.zip(expectedR._3.measurements).foreach { case (actualM, expectedM) =>
        actualM.params.axisData should contain theSameElementsAs expectedM.params.axisData.filterNot(_._1.isTransient)
        actualM.units should === (expectedM.units)
        actualM.value should === (expectedM.value)
        actualM.data.success should === (expectedM.data.success)
        actualM.data.complete should contain theSameElementsInOrderAs expectedM.data.complete
      }
    }
    actual.infomap should contain theSameElementsAs expected.infomap
  }
}
