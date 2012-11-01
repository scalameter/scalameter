package org.collperf
package collections



import collection._



class MapBenchmarks extends PerformanceTest.Regression with Collections {

  def persistor = new persistance.SerializationPersistor()

  /* tests */

  performance of "Map" in {

    measure method "apply" configuration (
      Key.minWarmupRuns -> 25,
      Key.benchRuns -> 30,
      Key.independentSamples -> 6,
      Key.significance -> 1e-13
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
        Key.benchRuns -> 48,
        Key.independentSamples -> 6,
        Key.frequency -> 2,
        Key.fullGC -> true,
        Key.noiseMagnitude -> 0.5,
        Key.retries -> 16
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

      using(treemaps(from, to, by)) curve("immutable.TreeMap") configuration (
        Key.benchRuns -> 128,
        Key.independentSamples -> 6,
        Key.frequency -> 2,
        Key.fullGC -> true,
        Key.noiseMagnitude -> 1.0
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
      Key.minWarmupRuns -> 25,
      Key.benchRuns -> 30,
      Key.independentSamples -> 6,
      Key.significance -> 1e-13
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
        Key.minWarmupRuns -> 20,
        Key.benchRuns -> 48,
        Key.independentSamples -> 6,
        Key.frequency -> 2,
        Key.fullGC -> true,
        Key.noiseMagnitude -> 0.5,
        Key.retries -> 16
      ) in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.get(i % sz)
          i += 1
        }
      }

      using(treemaps(from, to, by)) curve("immutable.TreeMap") configuration (
        Key.benchRuns -> 128,
        Key.independentSamples -> 4,
        Key.frequency -> 2,
        Key.fullGC -> true,
        Key.noiseMagnitude -> 1.0
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
      Key.minWarmupRuns -> 25,
      Key.benchRuns -> 48,
      Key.independentSamples -> 6,
      Key.frequency -> 4,
      Key.fullGC -> true,
      Key.significance -> 1e-13
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
      Key.minWarmupRuns -> 25,
      Key.benchRuns -> 36,
      Key.independentSamples -> 6,
      Key.frequency -> 3,
      Key.fullGC -> true,
      Key.significance -> 1e-13
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
      Key.minWarmupRuns -> 10,
      Key.benchRuns -> 30,
      Key.independentSamples -> 6,
      Key.significance -> 1e-13,
      Key.frequency -> 2,
      Key.fullGC -> true,
      Key.covMultiplier -> 1.5,
      Key.noiseMagnitude -> 0.5
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
      Key.minWarmupRuns -> 10,
      Key.benchRuns -> 30,
      Key.independentSamples -> 6,
      Key.significance -> 1e-13,
      Key.frequency -> 2,
      Key.fullGC -> true,
      Key.covMultiplier -> 1.5,
      Key.noiseMagnitude -> 0.5
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

      using(treemaps(from, to, by)) curve("immutable.TreeMap") in { xs =>
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

















