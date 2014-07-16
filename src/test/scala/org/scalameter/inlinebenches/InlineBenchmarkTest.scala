package org.scalameter.inlinebenches



import org.scalatest.FunSuite
import org.scalameter._



class InlineBenchmarkTest extends FunSuite {

  test("Should correctly execute an inline benchmark") {
    val time = inline.measure {
      for (i <- 0 until 100000) yield i
    }
    println(s"Total time: $time")
  }

  test("Should correctly execute an inline benchmark with warming") {
    val time = inline.config(
      Key.exec.benchRuns -> 20,
      Key.verbose -> true
    ) withWarmer {
      new Warmer.Default
    } withMeasurer {
      new Measurer.IgnoringGC
    } measure {
      for (i <- 0 until 100000) yield i
    }
    println(s"Total time: $time")
  }

  test("Should correctly measure memory footprint") {
    val mem = inline.config(
      Key.exec.benchRuns -> 20
    ) withMeasurer(new Measurer.MemoryFootprint) measure {
      for (i <- 0 until 100000) yield i
    }
    println(s"Total memory: $mem")
  }

}