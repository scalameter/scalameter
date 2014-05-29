package sctest



import org.scalameter.api._



object RangeBenchmark
extends PerformanceTest {

  /* configuration */

  lazy val executor = SeparateJvmsExecutor(Warmer.Default(), Aggregator.min, new Measurer.Default)
  lazy val reporter = ChartReporter(ChartFactory.XYLine())
  lazy val persistor = Persistor.None

  /* inputs */

  val sizes = Gen.range("size")(300000, 1500000, 300000)

  val ranges = for {
    size <- sizes
  } yield 0 until size

  /* tests */

  performance of "Range" in {
    measure method "map" in {
      using(ranges) in {
        r => r.map(_ + 1)
      }
    }
  }
}


object RangeBenchmark0
extends PerformanceTest.Quickbenchmark {

  /* configuration */

  /* inputs */

  val sizes = Gen.range("size")(300000, 1500000, 300000)

  val ranges = for {
    size <- sizes
  } yield 0 until size

  /* tests */

  performance of "Range" in {
    measure method "map" in {
      using(ranges) in {
        r => r.map(_ + 1)
      }
    }
  }
}


object RangeBenchmark1
extends PerformanceTest {

  /* configuration */

  lazy val executor = SeparateJvmsExecutor(Warmer.Default(), Aggregator.min, new Measurer.Default)
  lazy val reporter = new LoggingReporter
  lazy val persistor = Persistor.None

  /* inputs */

  val sizes = Gen.range("size")(300000, 1500000, 300000)

  val ranges = for {
    size <- sizes
  } yield 0 until size

  /* tests */

  performance of "Range" in {
    measure method "map" in {
      using(ranges) in {
        r => r.map(_ + 1)
      }
    }
  }
}


