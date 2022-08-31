package org.scalameter

import org.scalameter.examples._
import org.scalatest.funsuite.AnyFunSuite

class IncludeTest extends AnyFunSuite {
  test("include benchmark template") {
    try {
      (new TestSuite).executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

