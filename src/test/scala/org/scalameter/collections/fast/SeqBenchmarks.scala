package org.scalameter
package collections
package fast



import collection._
import Key._



class SeqBenchmarks extends Bench.Regression with Collections {

  def persistor = new persistence.SerializationPersistor()

  /* tests */

  performance of "Seq" in {

    measure method "apply" config (
      exec.minWarmupRuns -> 40,
      exec.maxWarmupRuns -> 120,
      exec.benchRuns -> 36,
      exec.independentSamples -> 3,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 100000
      val to = 1000000
      val by = 200000
      var sideeffect = 0

      using(arrays(from, to, by)) curve("Array") in { xs =>
        var i = 0
        var sum = 0
        val len = xs.length
        val until = len
        while (i < until) {
          sum += xs.apply(i % len)
          i += 1
        }
        sideeffect = sum
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer") in { xs =>
        var i = 0
        var sum = 0
        val len = xs.length
        val until = len
        while (i < until) {
          sum += xs.apply(i % len)
          i += 1
        }
        sideeffect = sum
      }

      using(vectors(from, to, by)) curve("Vector") in { xs =>
        var i = 0
        var sum = 0
        val len = xs.length
        val until = len
        while (i < until) {
          sum += xs.apply(i % len)
          i += 1
        }
        sideeffect = sum
      }

      using(ranges(from, to, by)) curve("Range") in { xs =>
        var i = 0
        var sum = 0
        val len = xs.length
        val until = len
        while (i < until) {
          sum += xs.apply(i % len)
          i += 1
        }
        sideeffect = sum
      }

    }

    measure method "update" config (
      exec.minWarmupRuns -> 60,
      exec.maxWarmupRuns -> 240,
      exec.benchRuns -> 36,
      exec.independentSamples -> 4,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 100000
      val to = 1000000
      val by = 200000
      var sideeffect = 0

      using(arrays(from, to, by)) curve("Array") in { xs =>
        var i = 0
        var sum = 0
        val len = xs.length
        val until = len
        while (i < until) {
          xs.update(i % len, i)
          i += 1
        }
        sideeffect = sum
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer") in { xs =>
        var i = 0
        var sum = 0
        val len = xs.length
        val until = len
        while (i < until) {
          xs.update(i % len, i)
          i += 1
        }
        sideeffect = sum
      }

    }

    measure method "append" config (
      exec.minWarmupRuns -> 60,
      exec.maxWarmupRuns -> 150,
      exec.benchRuns -> 36,
      exec.independentSamples -> 4,
      exec.outliers.suspectPercent -> 60,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 20000
      val to = 220000
      val by = 40000

      using(sizes(from, to, by)) curve("Vector") config (
        exec.benchRuns -> 32,
        exec.independentSamples -> 4,
        exec.outliers.suspectPercent -> 66,
        exec.outliers.covMultiplier -> 1.4
      ) in { len =>
        var i = 0
        var vector = Vector.empty[Int]
        while (i < len) {
          vector = vector :+ i
          i += 1
        }
      }

    }

    measure method "prepend" config (
      exec.minWarmupRuns -> 80,
      exec.maxWarmupRuns -> 200,
      exec.benchRuns -> 36,
      exec.independentSamples -> 4,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 50000
      val to = 250000
      val by = 50000

      using(sizes(from, to, by)) curve("Vector") config (
        exec.benchRuns -> 32,
        exec.independentSamples -> 4,
        exec.outliers.suspectPercent -> 66,
        exec.outliers.covMultiplier -> 1.4
      ) in { len =>
        var i = 0
        var vector = Vector.empty[Int]
        while (i < len) {
          vector = i +: vector
          i += 1
        }
      }

      using(sizes(from, to, by)) curve("List") config (
        exec.independentSamples -> 6,
        exec.outliers.suspectPercent -> 60,
        exec.outliers.covMultiplier -> 1.4
      ) in { len =>
        var i = 0
        var list = List.empty[Int]
        while (i < len) {
          list = i :: list
          i += 1
        }
      }

    }

    measure method "sorted" config (
      exec.minWarmupRuns -> 80,
      exec.maxWarmupRuns -> 200,
      exec.benchRuns -> 36,
      exec.independentSamples -> 4,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 20000
      val to = 1000000
      val by = 100000

      using(arrays(from, to, by)) curve("Array") in {
        _.sorted
      }

      using(vectors(from, to, by)) curve("Vector") in {
        _.sorted
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 32,
        exec.independentSamples -> 4,
        exec.outliers.suspectPercent -> 60,
        exec.outliers.covMultiplier -> 1.6
      ) in {
        _.sorted
      }

    }

  }

}

















