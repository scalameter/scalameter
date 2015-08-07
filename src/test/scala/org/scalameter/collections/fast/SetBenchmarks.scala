package org.scalameter
package collections
package fast



import collection._
import Key._



class SetBenchmarks extends Bench.Regression with Collections {

  def persistor = new persistence.SerializationPersistor()

  /* tests */

  performance of "Set" in {

    measure method "apply" config (
      exec.minWarmupRuns -> 80,
      exec.maxWarmupRuns -> 240,
      exec.warmupCovThreshold -> 0.15,
      exec.benchRuns -> 40,
      exec.independentSamples -> 4,
      exec.outliers.suspectPercent -> 60,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 10000
      val to = 60000
      val by = 10000
      
      using(hashtablesets(from, to, by)) curve("mutable.HashSet") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.apply(i)
          i += 1
        }
      }

      using(linkedhashtablesets(from, to, by)) curve("mutable.LinkedHashSet") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.apply(i)
          i += 1
        }
      }

      using(avlsets(from, to, by)) curve("mutable.TreeSet") in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.apply(i)
          i += 1
        }
      }

      using(hashtriesets(from, to, by)) curve("immutable.HashSet") config (
        exec.benchRuns -> 56,
        exec.independentSamples -> 8,
        exec.reinstantiation.frequency -> 4,
        exec.reinstantiation.fullGC -> true
      ) in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.apply(i)
          i += 1
        }
      }

      using(redblacksets(from, to, by)) curve("immutable.TreeSet") config (
        exec.minWarmupRuns -> 100,
        exec.maxWarmupRuns -> 300,
        exec.benchRuns -> 56,
        exec.independentSamples -> 8,
        exec.reinstantiation.frequency -> 3,
        exec.reinstantiation.fullGC -> true
      ) in { xs =>
        var i = 0
        val sz = xs.size
        val until = sz
        while (i < until) {
          xs.apply(i)
          i += 1
        }
      }

    }

    measure method "add" config (
      exec.minWarmupRuns -> 150,
      exec.maxWarmupRuns -> 450,
      exec.warmupCovThreshold -> 0.12,
      exec.benchRuns -> 32,
      exec.independentSamples -> 4,
      exec.outliers.suspectPercent -> 75,
      exec.outliers.covMultiplier -> 1.5,
      exec.reinstantiation.frequency -> 4,
      exec.reinstantiation.fullGC -> true,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 10000
      val to = 60000
      val by = 10000
      
      using(sizes(from, to, by)) curve("mutable.HashSet") in { sz =>
        var i = 0
        val xs = mutable.HashSet[Int]()
        while (i < sz) {
          xs.add(i)
          i += 1
        }
      }

      using(sizes(from, to, by)) curve("mutable.LinkedHashSet") in { sz =>
        var i = 0
        val xs = mutable.LinkedHashSet[Int]()
        while (i < sz) {
          xs.add(i)
          i += 1
        }
      }

      using(sizes(from, to, by)) curve("mutable.TreeSet") config (
        exec.minWarmupRuns -> 60,
        exec.maxWarmupRuns -> 180,
        exec.reinstantiation.frequency -> 4,
        exec.reinstantiation.fullGC -> true
      ) in { sz =>
        var i = 0
        val xs = mutable.TreeSet[Int]()
        while (i < sz) {
          xs.add(i)
          i += 1
        }
      }

    }

    measure method "update" config (
      exec.minWarmupRuns -> 120,
      exec.maxWarmupRuns -> 360,
      exec.benchRuns -> 32,
      exec.independentSamples -> 4,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 5000
      val to = 25000
      val by = 5000
      
      using(sized(hashtablesets(from, to, by))) curve("mutable.HashSet") tearDown {
        case (sz, xs) => for (i <- 0 until sz) xs.add(i)
      } in {
        case (sz, xs) =>
          var i = 0
          val until = sz * 4
          while (i < until) {
            xs.update(i % sz, i % 17 > 8)
            i += 1
          }
      }

      using(sized(avlsets(from, to, by))) curve("mutable.TreeSet") config (
        exec.benchRuns -> 30,
        exec.independentSamples -> 4
      ) tearDown {
        case (sz, xs) => for (i <- 0 until (sz / 8)) xs.add(i)
      } in {
        case (sz, xs) =>
          var i = 0
          val until = sz / 8
          while (i < until) {
            xs.update(i % sz, i % 17 > 8)
            i += 1
          }
      }
    }

    measure method "remove" config (
      exec.minWarmupRuns -> 120,
      exec.maxWarmupRuns -> 360,
      exec.warmupCovThreshold -> 0.12,
      exec.benchRuns -> 36,
      exec.independentSamples -> 4,
      exec.reinstantiation.frequency -> 4,
      exec.reinstantiation.fullGC -> true,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 20000
      val to = 60000
      val by = 10000
      
      using(sized(hashtablesets(from, to, by))) curve("mutable.HashSet") tearDown {
        case (sz, xs) => for (i <- 0 until sz) xs.add(i)
      } in {
        case (sz, xs) =>
          var i = 0
          val until = sz
          while (i < until) {
            xs.remove(i)
            i += 1
          }
      }

      using(sized(avlsets(from, to, by))) curve("mutable.TreeSet") tearDown {
        case (sz, xs) => for (i <- 0 until sz) xs.add(i)
      } in {
        case (sz, xs) =>
          var i = 0
          val until = sz
          while (i < until) {
            xs.remove(i)
            i += 1
          }
      }
    }

    measure method "+" config (
      exec.minWarmupRuns -> 80,
      exec.maxWarmupRuns -> 240,
      exec.benchRuns -> 36,
      exec.independentSamples -> 4,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 5000
      val to = 25000
      val by = 5000
      
      using(sizes(from, to, by)) curve("immutable.HashSet") in { sz =>
        var i = 0
        var xs = immutable.HashSet[Int]()
        while (i < sz) {
          xs += i
          i += 1
        }
      }

      using(sizes(from, to, by)) curve("immutable.TreeSet") in { sz =>
        var i = 0
        var xs = immutable.TreeSet[Int]()
        while (i < sz) {
          xs += i
          i += 1
        }
      }

    }

    measure method "-" config (
      exec.minWarmupRuns -> 150,
      exec.maxWarmupRuns -> 450,
      exec.benchRuns -> 32,
      exec.independentSamples -> 4,
      exec.outliers.suspectPercent -> 75,
      exec.outliers.covMultiplier -> 1.5,
      exec.outliers.retries -> 16,
      exec.reinstantiation.frequency -> 4,
      exec.reinstantiation.fullGC -> true,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 5000
      val to = 25000
      val by = 5000
      
      using(hashtriesets(from, to, by)) curve("immutable.HashSet") in { xs =>
        var i = 0
        val until = xs.size
        var s = xs
        while (i < until) {
          s -= i
          i += 1
        }
      }

      using(redblacksets(from, to, by)) curve("immutable.TreeSet") in { xs =>
        var i = 0
        val until = xs.size
        var s = xs
        while (i < until) {
          s -= i
          i += 1
        }
      }

    }

  }

}

















