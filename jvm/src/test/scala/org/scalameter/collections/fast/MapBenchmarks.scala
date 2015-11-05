package org.scalameter
package collections
package fast



import collection._
import Key._



class MapBenchmarks extends Bench.Regression with Collections {

  def persistor = new persistence.SerializationPersistor()

  /* tests */

  performance of "Map" in {

    measure method "apply" config (
      exec.minWarmupRuns -> 60,
      exec.maxWarmupRuns -> 180,
      exec.warmupCovThreshold -> 0.16,
      exec.benchRuns -> 30,
      exec.independentSamples -> 4,
      exec.outliers.suspectPercent -> 60,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 10000
      val to = 60000
      val by = 10000
      var sideeffect = 0

      using(hashtablemaps(from, to, by)) curve("mutable.HashMap") in { xs =>
        var i = 0
        var sum = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          sum += xs.apply(i % sz)
          i += 1
        }
        sideeffect = sum
      }

      using(linkedhashtablemaps(from, to, by)) curve("mutable.LinkedHashMap") in { xs =>
        var i = 0
        var sum = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          sum += xs.apply(i % sz)
          i += 1
        }
        sideeffect = sum
      }

      using(hashtriemaps(from, to, by)) curve("immutable.HashMap") config (
        exec.benchRuns -> 48,
        exec.independentSamples -> 6,
        exec.reinstantiation.frequency -> 2,
        exec.reinstantiation.fullGC -> true,
        exec.outliers.retries -> 16
      ) in { xs =>
        var i = 0
        var sum = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          sum += xs.apply(i % sz)
          i += 1
        }
        sideeffect = sum
      }

      using(redblackmaps(from, to, by)) curve("immutable.TreeMap") config (
        exec.benchRuns -> 64,
        exec.independentSamples -> 6,
        exec.reinstantiation.frequency -> 2,
        exec.reinstantiation.fullGC -> true
      ) in { xs =>
        var i = 0
        var sum = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          sum += xs.apply(i % sz)
          i += 1
        }
        sideeffect = sum
      }

    }

    measure method "get" config (
      exec.minWarmupRuns -> 60,
      exec.maxWarmupRuns -> 180,
      exec.warmupCovThreshold -> 0.16,
      exec.benchRuns -> 30,
      exec.independentSamples -> 6,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 10000
      val to = 60000
      val by = 10000

      using(hashtablemaps(from, to, by)) curve("mutable.HashMap") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.get(i % sz)
          i += 1
        }
      }

      using(linkedhashtablemaps(from, to, by)) curve("mutable.LinkedHashMap") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.get(i % sz)
          i += 1
        }
      }

      using(hashtriemaps(from, to, by)) curve("immutable.HashMap") config (
        exec.minWarmupRuns -> 20,
        exec.benchRuns -> 48,
        exec.independentSamples -> 6,
        exec.reinstantiation.frequency -> 2,
        exec.reinstantiation.fullGC -> true,
        exec.outliers.retries -> 16
      ) in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.get(i % sz)
          i += 1
        }
      }

      using(redblackmaps(from, to, by)) curve("immutable.TreeMap") config (
        exec.benchRuns -> 64,
        exec.independentSamples -> 4,
        exec.reinstantiation.frequency -> 2,
        exec.reinstantiation.fullGC -> true
      ) in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.get(i % sz)
          i += 1
        }
      }

    }

    measure method "update" config (
      exec.minWarmupRuns -> 80,
      exec.maxWarmupRuns -> 240,
      exec.warmupCovThreshold -> 0.15,
      exec.benchRuns -> 60,
      exec.independentSamples -> 5,
      exec.reinstantiation.frequency -> 4,
      exec.reinstantiation.fullGC -> true,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 10000
      val to = 80000
      val by = 15000

      using(hashtablemaps(from, to, by)) curve("mutable.HashMap") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.update(i % sz, i)
          i += 1
        }
      }

      using(juhashmaps(from, to, by)) curve("java.util.HashMap") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.put(i % sz, i)
          i += 1
        }
      }

      using(linkedhashtablemaps(from, to, by)) curve("mutable.LinkedHashMap") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.update(i % sz, i)
          i += 1
        }
      }
    }

    measure method "remove" config (
      exec.minWarmupRuns -> 80,
      exec.maxWarmupRuns -> 200,
      exec.benchRuns -> 50,
      exec.independentSamples -> 10,
      exec.reinstantiation.frequency -> 3,
      exec.reinstantiation.fullGC -> true,
      exec.outliers.suspectPercent -> 60,
      exec.outliers.covMultiplier -> 1.4,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 10000
      val to = 80000
      val by = 15000

      using(sized(hashtablemaps(from, to, by))) curve("mutable.HashMap") tearDown {
        case (sz, xs) => for (i <- 0 until sz) xs(i) = i
      } in {
        case (sz, xs) =>
          var i = 0
          while (i < sz) {
            xs.remove(i % sz)
            i += 1
          }
      }

      using(sized(juhashmaps(from, to, by))) curve("java.util.HashMap") tearDown {
        case (sz, xs) => for (i <- 0 until sz) xs.put(i, i)
      } in {
        case (sz, xs) =>
          var i = 0
          while (i < sz) {
            xs.remove(i % sz)
            i += 1
          }
      }

      using(sized(linkedhashtablemaps(from, to, by))) curve("mutable.LinkedHashMap") tearDown {
        case (sz, xs) => for (i <- 0 until sz) xs(i) = i
      } in {
        case (sz, xs) =>
          var i = 0
          while (i < sz) {
            xs.remove(i % sz)
            i += 1
          }
      }
    }

    measure method "+" config (
      exec.minWarmupRuns -> 60,
      exec.maxWarmupRuns -> 180,
      exec.benchRuns -> 64,
      exec.independentSamples -> 8,
      exec.reinstantiation.frequency -> 2,
      exec.reinstantiation.fullGC -> true,
      exec.outliers.suspectPercent -> 60,
      exec.outliers.covMultiplier -> 1.5,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.25
    ) in {
      val from = 5000
      val to = 25000
      val by = 5000

      using(sizes(from, to, by)) curve("immutable.HashMap") in { sz =>
        var i = 0
        var hm = immutable.HashMap[Int, Int]()
        while (i < sz) {
          hm += ((i, i))
          i += 1
        }
      }

      using(sizes(from, to, by)) curve("immutable.TreeMap") in { sz =>
        var i = 0
        var tm = immutable.TreeMap[Int, Int]()
        while (i < sz) {
          tm += ((i, i))
          i += 1
        }
      }

    }

    measure method "-" config (
      exec.minWarmupRuns -> 80,
      exec.maxWarmupRuns -> 240,
      exec.benchRuns -> 64,
      exec.independentSamples -> 8,
      exec.reinstantiation.frequency -> 2,
      exec.reinstantiation.fullGC -> true,
      exec.outliers.suspectPercent -> 60,
      exec.outliers.covMultiplier -> 1.5,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 5000
      val to = 25000
      val by = 5000

      using(hashtriemaps(from, to, by)) curve("immutable.HashMap") in { xs =>
        var i = 0
        var hm = xs
        val sz = xs.size
        while (i < sz) {
          hm -= i
          i += 1
        }
      }

      using(redblackmaps(from, to, by)) curve("immutable.TreeMap") in { xs =>
        var i = 0
        var tm = xs
        val sz = xs.size
        while (i < sz) {
          tm -= i
          i += 1
        }
      }

    }

  }

}

















