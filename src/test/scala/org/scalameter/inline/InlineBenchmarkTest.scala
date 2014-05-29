package org.scalameter.inline



import org.scalatest.FunSuite
import org.scalameter._



class InlineBenchmarkTest extends FunSuite {

  test("Should correctly execute an inline benchmark") {
    val time = measure() { u =>
      for (i <- 0 until 100000) yield i
    }
    println(s"Total time: $time")
  }

}