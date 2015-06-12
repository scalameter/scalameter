package org.scalameter.persistence

import org.scalameter._
import org.scalameter.reporting.RegressionReporter


class RangeBenchmark(val persistor: org.scalameter.Persistor) extends PerformanceTest {
  val sizes = Gen.range("size")(300000, 1500000, 300000)

  val ranges = for {
    size <- sizes
  } yield 0 until size

  performance of "Range" in {
    measure method "map" in {
      using(ranges) in {
        r => r.map(_ + 1)
      }
    }
  }

  def executor = new execution.LocalExecutor(
    Warmer.Default(),
    Aggregator.min,
    measurer
  )
  def measurer = new Measurer.Default
  def reporter = new reporting.RegressionReporter(RegressionReporter.Tester.Accepter(), RegressionReporter.Historian.ExponentialBackoff())
}
