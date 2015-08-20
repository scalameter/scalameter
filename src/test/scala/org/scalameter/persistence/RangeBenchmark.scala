package org.scalameter.persistence

import org.scalameter._
import org.scalameter.reporting.RegressionReporter


class RangeBenchmark(override val persistor: Persistor) extends Bench.LocalTime {
  override def reporter = new reporting.RegressionReporter(
    RegressionReporter.Tester.Accepter(),
    RegressionReporter.Historian.Complete()
  )

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
}
