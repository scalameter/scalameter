package org.scalameter

import org.scalameter.examples._
import org.scalatest.FunSuite

class IncludeTest extends FunSuite {
  test("include benchmark template") {
    try {
      new TestSuite executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

