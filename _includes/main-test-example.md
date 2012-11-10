
    import org.scalameter.api._

    object RangeBenchmark
    extends PerformanceTest.Microbenchmark {
      val ranges = for {
        size <- Gen.range("size")(300000, 1500000, 300000)
      } yield 0 until size

      measure method "map" in {
        using(ranges) curve("Range") in {
          _.map(_ + 1)
        }
      }
    }