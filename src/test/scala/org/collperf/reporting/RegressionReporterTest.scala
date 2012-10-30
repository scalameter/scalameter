package org.collperf
package reporting



import java.util.Date
import org.scalatest.FunSuite



class RegressionReporterTest extends FunSuite {

  test("ExponentialBackoff should correctly prune the history") {
    val expback = RegressionReporter.Historian.ExponentialBackoff()
    val history = History(Seq(
      (new Date(1000000), Context.empty, Seq()),
      (new Date(2000000), Context.empty, Seq()),
      (new Date(3000000), Context.empty, Seq()),
      (new Date(4000000), Context.empty, Seq())
    ))

    val h1 = expback.prune(history, (new Date(5000000), Context.empty, Seq()))
    assert(h1 === History(Seq(
      (new Date(1000000), Context.empty, Seq()),
      (new Date(2000000), Context.empty, Seq()),
      (new Date(3000000), Context.empty, Seq()),
      (new Date(4000000), Context.empty, Seq()),
      (new Date(5000000), Context.empty, Seq())
    ), Map(Key.timeIndices -> Seq(1, 2, 3, 5, 9))))

    val h2 = expback.prune(h1, (new Date(6000000), Context.empty, Seq()))
    assert(h2 === History(Seq(
      (new Date(1000000), Context.empty, Seq()),
      (new Date(2000000), Context.empty, Seq()),
      (new Date(3000000), Context.empty, Seq()),
      (new Date(5000000), Context.empty, Seq()),
      (new Date(6000000), Context.empty, Seq())
    ), Map(Key.timeIndices -> Seq(1, 2, 4, 6, 10))))

    val h3 = expback.prune(h2, (new Date(7000000), Context.empty, Seq()))
    assert(h3 === History(Seq(
      (new Date(1000000), Context.empty, Seq()),
      (new Date(2000000), Context.empty, Seq()),
      (new Date(5000000), Context.empty, Seq()),
      (new Date(6000000), Context.empty, Seq()),
      (new Date(7000000), Context.empty, Seq())
    ), Map(Key.timeIndices -> Seq(1, 2, 3, 7, 11))))

    val h4 = expback.prune(h3, (new Date(8000000), Context.empty, Seq()))
    assert(h4 === History(Seq(
      (new Date(1000000), Context.empty, Seq()),
      (new Date(2000000), Context.empty, Seq()),
      (new Date(5000000), Context.empty, Seq()),
      (new Date(7000000), Context.empty, Seq()),
      (new Date(8000000), Context.empty, Seq())
    ), Map(Key.timeIndices -> Seq(1, 2, 4, 8, 12))))
  }

}



