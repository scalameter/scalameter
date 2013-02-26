package org.scalameter



import org.scalameter.api._
import collection._
//import reporting._
import java.awt.Color

//object JSChartTest extends App {
//  new JSChartTest().executeTests()
//}

class JSChartTest extends PerformanceTest {

  lazy val tester = RegressionReporter.Tester.Accepter()
  lazy val executor = execution.LocalExecutor(Executor.Warmer.Default(), Aggregator.complete(Aggregator.average), new Executor.Measurer.Default)
  //lazy val reporter: Reporter = ChartReporter(ChartFactory.NormalHistogram())
  lazy val reporter = Reporter.Composite(
    new DsvReporter('\t'),
    HtmlReporter(HtmlReporter.Renderer.Info(), HtmlReporter.Renderer.JSChart()),
    RegressionReporter(tester, RegressionReporter.Historian.Window(5))
  )

  lazy val persistor = new persistence.SerializationPersistor()

  val sizes = Gen.range("size")(300000, 900000, 300000)
  
  val ranges = for(sz <- sizes) yield (0 until sz)
  val arrays = for (sz <- sizes) yield (0 until sz).toArray
  val lists = for (sz <- sizes) yield (0 until sz).toList
  val arrays2 = for {
    sz <- sizes
    offset <- Gen.range("offset")(100000, 500000, 200000)
  } yield {
    val a = (0 until sz).toArray
    a.map(_ + offset)
  }

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
    
    measure method "sum" in {

      using(ranges) curve("Ranges") in { range =>
        range.sum
      }

      using(arrays) curve("Arrays") in { array =>
        array.sum
      }

      using(lists) curve("Lists") in { list =>
        list.sum
      }

    }
  }
}





























