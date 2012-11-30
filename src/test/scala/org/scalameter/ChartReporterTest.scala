package org.scalameter


import org.scalameter.api._
import collection._
//import reporting._
import java.awt.Color


/**
 * Test for the ConfidenceIntervals (Regression) chart factory
 */
class ChartReporterTest extends PerformanceTest {

  lazy val executor = execution.LocalExecutor(Executor.Warmer.Default(), Aggregator.complete(Aggregator.average), new Executor.Measurer.Default)
  lazy val colorsTestSample = List(new Color(0, 0, 255), new Color(255, 255, 0))
  lazy val reporter = Reporter.Composite(
    //ChartReporter(ChartReporter.ChartFactory.Regression(true, true, 0.001), "chart_"),
    HtmlReporter(HtmlReporter.Renderer.Info(), HtmlReporter.Renderer.Regression(ChartReporter.ChartFactory.ConfidenceIntervals(true, true, 0.001), colorsTestSample)),
    RegressionReporter(RegressionReporter.Tester.Accepter(), RegressionReporter.Historian.Complete())
  )
  lazy val persistor = new persistence.SerializationPersistor()

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

/**
 * Test for the Histograms chart factory
 */
class TrendHistogramTest extends PerformanceTest {

  lazy val executor = execution.LocalExecutor(Executor.Warmer.Default(), Aggregator.complete(Aggregator.average), new Executor.Measurer.Default)
  lazy val colorsTestSample = List(new Color(0, 0, 255), new Color(255, 255, 0))
  //lazy val reporter: Reporter = ChartReporter(ChartFactory.TrendHistogram())
  lazy val reporter = Reporter.Composite(
    ChartReporter(ChartFactory.TrendHistogram()),
    RegressionReporter(RegressionReporter.Tester.Accepter(), RegressionReporter.Historian.Window(5))
  )
  lazy val persistor = new persistence.SerializationPersistor()

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


