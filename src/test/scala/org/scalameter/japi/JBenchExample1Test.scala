package org.scalameter.japi

import org.scalameter.BasePerformanceTest._
import org.scalameter.api._
import org.scalameter.examples.JBenchExample1
import org.scalameter.utils.Tree
import org.scalameter.{Context, Gen, MethodCounter, Setup}
import org.scalatest.{FunSuite, Matchers}



class JBenchExample1Test extends FunSuite with Matchers {
  private val benchs = 20
  private val warmups = 10
  private val gen = Gen.range("size")(10000, 100000, 30000).map(new Array[Int](_))

  private def checkSetupTree(setupTree: Tree[Setup[_]]): Unit = {
    var expectedCurves = Set("sum", "multiply")
    setupTree.context should === (Context.topLevel + (verbose -> false))
    setupTree.foreach { setup =>
      (setup.context - dsl.curve) should === (Context.topLevel ++ Context(
        verbose -> false,
        exec.benchRuns -> benchs,
        exec.maxWarmupRuns -> warmups,
        dsl.scope -> List("forloops", "arrays")
      ))
      expectedCurves.contains(setup.context(dsl.curve)) should === (true)
      expectedCurves -= setup.context(dsl.curve)
    }

    setupTree.foreach { setup =>
      setup.gen.dataset.toList should
        contain theSameElementsAs gen.dataset.toList
      setup.gen.dataset.zip(gen.dataset).foreach { case (a, e) =>
        setup.gen.generate(a).asInstanceOf[Array[Int]] should
          contain theSameElementsInOrderAs gen.generate(e)
      }
    }
  }

  private def checkCounts(counts: Map[String, Int]): Unit = {
    counts.foreach {
      case ("initRandom1", count) =>
        count should === (2)
      case ("destroyRandom1", count) =>
        count should === (2)
      case ("warmupRandom1", count) =>
        count should === (gen.warmupset.length + warmups)
      case ("doNothing1", count) =>
        count should === (gen.dataset.length * benchs)
      case ("fillArray1", count) =>
        count should === (gen.dataset.length * benchs)
      case ("initRandom2", count) =>
        count should === (2)
      case ("destroyRandom2", count) =>
        count should === (2)
      case ("warmupRandom2", count) =>
        count should === (gen.warmupset.length + warmups)
      case ("doNothing2", count) =>
        count should === (gen.dataset.length * benchs)
      case ("fillArray2", count) =>
        count should === (gen.dataset.length * benchs)
      case _ =>
    }
  }

  test("JBenchExample should be constructed and executed correctly") {
    MethodCounter.reset()

    val example = new JBenchExample1
    checkSetupTree(setupzipper.value.result)
    example.executeTests()
    checkCounts(MethodCounter.counts)

    MethodCounter.reset()
  }
}
