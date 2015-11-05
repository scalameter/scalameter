package org.scalameter.japi

import org.scalameter.examples.JBenchExample2
import org.scalatest.FunSuite


class JBenchExample2Test extends FunSuite{
  test("JBench regression report example should correctly execute") {
    try {
      val test = new JBenchExample2
      test.executeTests()
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }
  }
}
