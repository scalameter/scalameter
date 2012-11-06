package org.scalameter
package collections



import collection._
import Key._



class MapBenchmarks extends PerformanceTest.Regression with Collections {

  def persistor = new persistance.SerializationPersistor()

  /* tests */

  performance of "Map" in {

    measure method "apply" configuration (
      exec.minWarmupRuns -> 25,
      exec.benchRuns -> 30,
      exec.independentSamples -> 6,
      reporting.regression.significance -> 1e-13
    ) in {
      val from = 50000
      val to = 500000
      val by = 100000
      var sideeffect = 0

      using(hashtablemaps(from, to, by)) curve("mutable.HashMap") in { xs =>
        var i = 0
        var sum = 0
        val sz = xs.size
        val until = sz * 2
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
        val until = sz * 2
        while (i < until) {
          sum += xs.apply(i % sz)
          i += 1
        }
        sideeffect = sum
      }

      using(hashtriemaps(from, to, by)) curve("immutable.HashMap") configuration (
        exec.benchRuns -> 48,
        exec.independentSamples -> 6,
        exec.reinstantiation.frequency -> 2,
        exec.reinstantiation.fullGC -> true,
        exec.noise.magnitude -> 0.5,
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

      using(redblackmaps(from, to, by)) curve("immutable.TreeMap") configuration (
        exec.benchRuns -> 128,
        exec.independentSamples -> 6,
        exec.reinstantiation.frequency -> 2,
        exec.reinstantiation.fullGC -> true,
        exec.noise.magnitude -> 1.0
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

    measure method "get" configuration (
      exec.minWarmupRuns -> 25,
      exec.benchRuns -> 30,
      exec.independentSamples -> 6,
      reporting.regression.significance -> 1e-13
    ) in {
      val from = 50000
      val to = 500000
      val by = 100000

      using(hashtablemaps(from, to, by)) curve("mutable.HashMap") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz * 2
        while (i < until) {
          xs.get(i % sz)
          i += 1
        }
      }

      using(linkedhashtablemaps(from, to, by)) curve("mutable.LinkedHashMap") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz * 2
        while (i < until) {
          xs.get(i % sz)
          i += 1
        }
      }

      using(hashtriemaps(from, to, by)) curve("immutable.HashMap") configuration (
        exec.minWarmupRuns -> 20,
        exec.benchRuns -> 48,
        exec.independentSamples -> 6,
        exec.reinstantiation.frequency -> 2,
        exec.reinstantiation.fullGC -> true,
        exec.noise.magnitude -> 0.5,
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

      using(redblackmaps(from, to, by)) curve("immutable.TreeMap") configuration (
        exec.benchRuns -> 128,
        exec.independentSamples -> 4,
        exec.reinstantiation.frequency -> 2,
        exec.reinstantiation.fullGC -> true,
        exec.noise.magnitude -> 1.0
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

    measure method "update" configuration (
      exec.minWarmupRuns -> 25,
      exec.benchRuns -> 48,
      exec.independentSamples -> 6,
      exec.reinstantiation.frequency -> 4,
      exec.reinstantiation.fullGC -> true,
      reporting.regression.significance -> 1e-13
    ) in {
      val from = 50000
      val to = 500000
      val by = 100000

      using(hashtablemaps(from, to, by)) curve("mutable.HashMap") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz * 2
        while (i < until) {
          xs.update(i % sz, i)
          i += 1
        }
      }

      using(linkedhashtablemaps(from, to, by)) curve("mutable.LinkedHashMap") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz * 2
        while (i < until) {
          xs.update(i % sz, i)
          i += 1
        }
      }
    }

    measure method "remove" configuration (
      exec.minWarmupRuns -> 25,
      exec.benchRuns -> 36,
      exec.independentSamples -> 6,
      exec.reinstantiation.frequency -> 3,
      exec.reinstantiation.fullGC -> true,
      reporting.regression.significance -> 1e-13
    ) in {
      val from = 50000
      val to = 500000
      val by = 100000

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

    measure method "+" configuration (
      exec.minWarmupRuns -> 10,
      exec.benchRuns -> 30,
      exec.independentSamples -> 6,
      reporting.regression.significance -> 1e-13,
      exec.reinstantiation.frequency -> 2,
      exec.reinstantiation.fullGC -> true,
      exec.outliers.covMultiplier -> 1.5,
      exec.noise.magnitude -> 0.5
    ) in {
      val from = 50000
      val to = 300000
      val by = 50000

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

    measure method "-" configuration (
      exec.minWarmupRuns -> 10,
      exec.benchRuns -> 30,
      exec.independentSamples -> 6,
      reporting.regression.significance -> 1e-13,
      exec.reinstantiation.frequency -> 2,
      exec.reinstantiation.fullGC -> true,
      exec.outliers.covMultiplier -> 1.5,
      exec.noise.magnitude -> 0.5
    ) in {
      val from = 50000
      val to = 300000
      val by = 50000

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

















