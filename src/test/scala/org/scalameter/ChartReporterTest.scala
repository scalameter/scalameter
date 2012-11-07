package org.scalameter



import collection._
import reporting._



class ChartReporterTest extends PerformanceTest {

  lazy val executor = execution.LocalExecutor(Aggregator.complete(Aggregator.average), new Executor.Measurer.Default)
  lazy val reporter = Reporter.Composite(
    ChartReporter("chart_", ChartReporter.ChartFactory.Regression(true, true, 0.001)),
    RegressionReporter(RegressionReporter.Tester.Accepter(), RegressionReporter.Historian.Complete())
  )
  lazy val persistor = new persistance.SerializationPersistor()

  val sizes = Gen.range("size")(300000, 1500000, 300000)

  performance of "Range" in {

    measure method "map" in {

      using(sizes) curve("Range") in { sz =>
        val range = 0 until sz
        range.map(_ + 1)
      }

    }

    measure method "filter" in {

      using(sizes) curve("Range") in { sz =>
        val range = 0 until sz
        range.filter(_ % 2 == 0)
      }

    }

  }

}


