package org.scalameter


import org.scalameter.api._
import collection._
//import reporting._
import java.awt.Color


/**
 * Test for the ConfidenceIntervals (Regression) chart factory
 */
class ConfidenceIntervalsChartTest extends PerformanceTest {

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

    measure method "zipWithIndex" in {

      using(sizes) curve("Range") in { sz =>
        val range = 0 until sz
        range.zipWithIndex
      }

    }

  }

}

/**
 * Test for the Histograms chart factory
 */
class TrendHistogramTest extends PerformanceTest {

  lazy val executor = execution.LocalExecutor(Executor.Warmer.Default(), Aggregator.complete(Aggregator.average), new Executor.Measurer.Default)
  lazy val colorsTestSample = List(new Color(194, 27, 227), new Color(196, 214, 0), new Color(14, 201, 198), new Color(212, 71, 11))
  //lazy val reporter: Reporter = ChartReporter(ChartFactory.TrendHistogram())
  lazy val reporter = Reporter.Composite(
    //ChartReporter(ChartFactory.TrendHistogram()),
    HtmlReporter(HtmlReporter.Renderer.Info(), HtmlReporter.Renderer.Histogram(ChartReporter.ChartFactory.TrendHistogram(), colorsTestSample)),
    RegressionReporter(RegressionReporter.Tester.Accepter(), RegressionReporter.Historian.Window(5))
  )

  lazy val persistor = new persistence.SerializationPersistor()

  val sizes = Gen.range("size")(300000, 900000, 300000)
  
  val ranges = for(sz <- sizes) yield (0 until sz)
  val arrays = for (sz <- sizes) yield (0 until sz).toArray
  val lists = for (sz <- sizes) yield (0 until sz).toList

  performance of "CollectionMethods" config {

    Key.exec.jvmflags -> "-Xmx1024m -Xms1024m"

  } in {

    measure method "map" in {

      using(ranges) curve("Ranges") in { range =>
        range.map(_ + 1)
      }

      using(arrays) curve("Arrays") in { array =>
        array.map(_ + 1)
      }

      using(lists) curve("Lists") in { list =>
        list.map(_ + 1)
      }

    }
  }
}

class NormalHistogramTest extends PerformanceTest {

  lazy val executor = execution.LocalExecutor(Executor.Warmer.Default(), Aggregator.complete(Aggregator.average), new Executor.Measurer.Default)
  //lazy val reporter: Reporter = ChartReporter(ChartFactory.NormalHistogram())
  lazy val reporter = Reporter.Composite(
    ChartReporter(ChartFactory.NormalHistogram()),
    RegressionReporter(RegressionReporter.Tester.Accepter(), RegressionReporter.Historian.Window(5))
  )

  lazy val persistor = new persistence.SerializationPersistor()

  val sizes = Gen.range("size")(300000, 900000, 300000)
  
  val ranges = for(sz <- sizes) yield (0 until sz)
  val arrays = for (sz <- sizes) yield (0 until sz).toArray
  val lists = for (sz <- sizes) yield (0 until sz).toList

  performance of "CollectionMethods" config {

    Key.exec.jvmflags -> "-Xmx1024m -Xms1024m"

  } in {

    measure method "map" in {

      using(ranges) curve("Ranges") in { range =>
        range.map(_ + 1)
      }

      using(arrays) curve("Arrays") in { array =>
        array.map(_ + 1)
      }

      using(lists) curve("Lists") in { list =>
        list.map(_ + 1)
      }

    }
  }
}


