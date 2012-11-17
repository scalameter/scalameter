package org.scalameter



import collection._
import reporting._
import java.awt.Color


class ChartReporterTest extends PerformanceTest {

  lazy val executor = execution.LocalExecutor(Aggregator.complete(Aggregator.average), new Executor.Measurer.Default)
  lazy val colorsTestSample = List(new Color(0, 0, 255), new Color(255, 255, 0))
  lazy val reporter = Reporter.Composite(
    //ChartReporter(ChartReporter.ChartFactory.Regression(true, true, 0.001), "chart_"),
    HtmlReporter(HtmlReporter.Renderer.Info(), HtmlReporter.Renderer.Regression(ChartReporter.ChartFactory.ConfidenceIntervals(true, true, 0.001), colorsTestSample)),
    RegressionReporter(RegressionReporter.Tester.Accepter(), RegressionReporter.Historian.Complete())
  )
  lazy val persistor = new persistance.SerializationPersistor()

  val sizes = Gen.range("size")(300000, 1500000, 300000)

  performance of "Range" config {
    Key.exec.jvmflags -> "-Xmx1024m -Xms1024m"
  } in {

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


