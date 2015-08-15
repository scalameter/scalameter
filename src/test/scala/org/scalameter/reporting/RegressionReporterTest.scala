package org.scalameter
package reporting



import java.util.Date
import org.scalatest.FunSuite



class RegressionReporterTest extends FunSuite {

  import Key.reports.regression.timeIndices

  test("ExponentialBackoff should correctly prune the history") {
    val expback = RegressionReporter.Historian.ExponentialBackoff()

    val afterEmpty = expback.push(History[Double](Seq()), (new Date(0), Context.empty, CurveData.empty[Double]))
    assert(afterEmpty === History[Double](Seq(
      (new Date(0), Context.empty, CurveData.empty[Double])
    ), Map(timeIndices -> Seq(1))))

    val history = History[Double](Seq(
      (new Date(1000000), Context.empty, CurveData.empty[Double]),
      (new Date(2000000), Context.empty, CurveData.empty[Double]),
      (new Date(3000000), Context.empty, CurveData.empty[Double]),
      (new Date(4000000), Context.empty, CurveData.empty[Double])
    ))

    val h1 = expback.push(history, (new Date(5000000), Context.empty, CurveData.empty[Double]))
    assert(h1 === History[Double](Seq(
      (new Date(1000000), Context.empty, CurveData.empty[Double]),
      (new Date(2000000), Context.empty, CurveData.empty[Double]),
      (new Date(3000000), Context.empty, CurveData.empty[Double]),
      (new Date(4000000), Context.empty, CurveData.empty[Double]),
      (new Date(5000000), Context.empty, CurveData.empty[Double])
    ), Map(timeIndices -> Seq(1, 2, 3, 5, 9))))

    val h2 = expback.push(h1, (new Date(6000000), Context.empty, CurveData.empty[Double]))
    assert(h2 === History[Double](Seq(
      (new Date(1000000), Context.empty, CurveData.empty[Double]),
      (new Date(2000000), Context.empty, CurveData.empty[Double]),
      (new Date(3000000), Context.empty, CurveData.empty[Double]),
      (new Date(5000000), Context.empty, CurveData.empty[Double]),
      (new Date(6000000), Context.empty, CurveData.empty[Double])
    ), Map(timeIndices -> Seq(1, 2, 4, 6, 10))))

    val h3 = expback.push(h2, (new Date(7000000), Context.empty, CurveData.empty[Double]))
    assert(h3 === History[Double](Seq(
      (new Date(1000000), Context.empty, CurveData.empty[Double]),
      (new Date(2000000), Context.empty, CurveData.empty[Double]),
      (new Date(5000000), Context.empty, CurveData.empty[Double]),
      (new Date(6000000), Context.empty, CurveData.empty[Double]),
      (new Date(7000000), Context.empty, CurveData.empty[Double])
    ), Map(timeIndices -> Seq(1, 2, 3, 7, 11))))

    val h4 = expback.push(h3, (new Date(8000000), Context.empty, CurveData.empty[Double]))
    assert(h4 === History[Double](Seq(
      (new Date(1000000), Context.empty, CurveData.empty[Double]),
      (new Date(2000000), Context.empty, CurveData.empty[Double]),
      (new Date(5000000), Context.empty, CurveData.empty[Double]),
      (new Date(7000000), Context.empty, CurveData.empty[Double]),
      (new Date(8000000), Context.empty, CurveData.empty[Double])
    ), Map(timeIndices -> Seq(1, 2, 4, 8, 12))))
  }

}



