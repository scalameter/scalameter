package org.scalameter.examples



import org.scalameter.api._
import org.scalameter.picklers.Implicits._



class RegressionTest extends Bench.OfflineReport {

  val sizes = Gen.range("size")(1000000, 5000000, 2000000)

  val arrays = for (sz <- sizes) yield (0 until sz).toArray

  performance of "Array" in {
    measure method "foreach" in {
      using(arrays) config (
        exec.independentSamples -> 1
      ) in { xs =>
        var sum = 0
        xs.foreach(x => sum += x)
      }
    }
  }

}


class RegressionTest2 extends Bench.OfflineReport {

  val sizes = Gen.range("size")(1000000, 2000000, 500000)

  val lists = for (sz <- sizes) yield (0 until sz).toList

  performance of "List" in {
    measure method "map" in {
      using(lists) config (
        exec.benchRuns -> 20,
        exec.independentSamples -> 1,
        exec.reinstantiation.frequency -> 2
      ) in { xs =>
        xs.map(_ + 1)
      }
    }
  }

}


trait RegressionTest3 extends Bench.OfflineReport {

  val sizes = Gen.single("size")(500000)

  val lists = for (sz <- sizes) yield (0 until sz).toList

  performance of "List" in {
    measure method "groupBy" in {
      using(lists) config (
        exec.benchRuns -> 20,
        exec.independentSamples -> 1,
        exec.outliers.covMultiplier -> 1.5,
        exec.outliers.suspectPercent -> 40
      ) in { xs =>
        xs.groupBy(_ % 10)
      }
    }
  }

}










