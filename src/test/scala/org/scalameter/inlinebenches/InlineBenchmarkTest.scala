package org.scalameter.inlinebenches



import org.scalatest.FunSuite
import org.scalameter._



class InlineBenchmarkTest extends FunSuite {

  test("Should correctly execute an inline benchmark") {
    val time = measure {
      for (i <- 0 until 100000) yield i
    }
    println(s"Total time: $time")
  }

  test("Should correctly execute an inline benchmark with warming") {
    val time = config(
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
    val mem = config(
      Key.exec.benchRuns -> 20
    ) withMeasurer(new Measurer.MemoryFootprint) measure {
      for (i <- 0 until 100000) yield i
    }
    println(s"Total memory: $mem")
  }

  test("Should correctly measure gc cycles") {
    val gc = config(
      Key.exec.benchRuns -> 30
    ) withMeasurer(new Measurer.GarbageCollectionCycles, Aggregator.median[Int]) measure {
      for (i <- 0 until 15000000) yield i
    }
    println(s"Total gcs: $gc")
  }

}