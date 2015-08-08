package org.scalameter.examples

import org.scalameter.api._
import org.scalameter.execution.invocation.InvocationCountMatcher
import org.scalameter.persistence.InterceptingPersistor


class InvocationCountMeasurerBench extends PerformanceTest.OnlineRegressionReport {
  override val persistor: InterceptingPersistor = new InterceptingPersistor(new GZIPJSONSerializationPersistor)

  def min: Int = 5000

  def hop: Int = 5000

  def max: Int = 50000

  val sizes = Gen.range("size")(min, max, hop)

  val lists = for (sz <- sizes) yield (0 until sz).toList
}

class BoxingCountTest extends InvocationCountMeasurerBench {
  override lazy val measurer: Measurer = Measurer.BoxingCount.all()

  override def defaultConfig = Context(
    exec.independentSamples -> 1
  )

  performance of "List" in {
    measure method "map" in {
      using(lists) in { xs =>
        xs.map(_ + 1)
      }
    }
  }
}

class MethodInvocationCountTest1 extends InvocationCountMeasurerBench {
  override lazy val measurer: Measurer = Measurer.MethodInvocationCount(
    InvocationCountMatcher.allocations(classOf[Some[_]])
  )

  override def defaultConfig = Context(
    exec.independentSamples -> 1
  )

  performance of "List" in {
    measure method "map" in {
      using(lists) in { xs =>
        xs.map(Some(_))
      }
    }
  }
}
