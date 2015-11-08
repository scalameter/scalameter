package org.scalameter



import org.scalameter.examples.InvocationCountMeasurerBench
import org.scalatest.{Matchers, FunSuite}



abstract class InvocationCountMeasurerTest extends FunSuite with Matchers {
  def checkInvocationCountMeasurerTest(test: InvocationCountMeasurerBench): Any = {
    try {
      test.executeTests()
      try {
        for ((ctx, history) <- test.persistor.intercepted) {
          history.results.length should === (1)
          history.results.head._3.measurements.length should === ((test.max - test.min) / test.hop + 1)
          history.results.head._3.measurements.foldLeft(test.min.toDouble) { (expected, measurement) =>
            measurement.value should === (expected)
            expected + test.hop
          }
        }
      } finally {
        for ((ctx, history) <- test.persistor.intercepted) {
          test.persistor.fileFor(ctx).delete()
        }
      }
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }
  }
}
