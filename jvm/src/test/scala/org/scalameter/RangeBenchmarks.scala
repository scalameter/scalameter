package sctest



import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.Pickler



object RangeBenchmark
extends Bench[Double] {
  /* configuration */

  lazy val measurer = new Measurer.Default

  lazy val executor = SeparateJvmsExecutor(Warmer.Default(), Aggregator.min[Double], measurer)
  lazy val reporter = ChartReporter[Double](ChartFactory.XYLine())
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
extends Bench.LocalTime {

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
extends Bench[Double] {
  /* configuration */

  lazy val measurer = new Measurer.Default

  lazy val executor = SeparateJvmsExecutor(Warmer.Default(), Aggregator.min[Double], measurer)
  lazy val reporter = new LoggingReporter[Double]
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


