package org.scalameter

import collection._
import reporting._
import Key._
import collections._
import parallel.ParIterable



class JSChartTest extends PerformanceTest.Regression with Collections {

  def persistor = new persistence.SerializationPersistor()

  override def reporter: Reporter = org.scalameter.Reporter.Composite(
    new RegressionReporter(RegressionReporter.Tester.OverlapIntervals(), RegressionReporter.Historian.ExponentialBackoff()),
    new DsvReporter('\t'),
    HtmlReporter(HtmlReporter.Renderer.JSChart())
  )

  /* tests */

  // val heapsizes = Gen.range("heapsize")(512, 2048, 512)
  // val jvmflags = for (heapsize <- heapsizes) yield s"-Xmx${heapsize}m -Xms${heapsize}m"

  // def withFlags(sizes: Gen[_]) = Gen.tupled(jvmflags, sizes)
  def withPar[A <% CustomParallelizable[B, C[B]], B, C[B] <: ParIterable[B]](collections: Gen[A]) = {
    for {
      collection <- collections
      par <- Gen.exponential("par")(1, 8, 2)
    } yield {
      val parCollection = collection.par
      parCollection.tasksupport = new parallel.ForkJoinTaskSupport(
        new scala.concurrent.forkjoin.ForkJoinPool(par))
      parCollection
    }
  }

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
  }

  /* traversable collections */

  /*

  performance of "Traversable" in {

    measure method "reduce" config (
      exec.minWarmupRuns -> 120,
      exec.maxWarmupRuns -> 240,
      exec.benchRuns -> 36,
      exec.independentSamples -> 3,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 100000
      val to = 600000
      val by = 150000

      using(withPar(arrays(from, to, by))) curve("Array") config (
        exec.minWarmupRuns -> 150,
        exec.maxWarmupRuns -> 320
      ) in {
        _.reduce(_ + _)
      }

      using(withPar(arraybuffers(from, to, by))) curve("ArrayBuffer") in {
        _.reduce(_ + _)
      }

      using(withPar(vectors(from, to, by))) curve("Vector") in {
        _.reduce(_ + _)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 30,
        exec.independentSamples -> 4,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 5
      ) in {
        _.reduce(_ + _)
      }

      using(withPar(ranges(from, to, by))) curve("Range") in {
        _.reduce(_ + _)
      }
    }
    
    measure method "filter" config (
      exec.minWarmupRuns -> 100,
      exec.maxWarmupRuns -> 200,
      exec.benchRuns -> 36,
      exec.independentSamples -> 4,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 100000
      val to = 400000
      val by = 100000

      using(withPar(arrays(from, to, by))) curve("Array") in {
        _.filter(_ % 2 == 0)
      }

      using(withPar(arraybuffers(from, to, by))) curve("ArrayBuffer") config (
        exec.minWarmupRuns -> 120,
        exec.maxWarmupRuns -> 240,
        exec.reinstantiation.frequency -> 4
      ) in {
        _.filter(_ % 2 == 0)
      }
      
      using(withPar(vectors(from, to, by))) curve("Vector") in {
        _.filter(_ % 2 == 0)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.minWarmupRuns -> 120,
        exec.maxWarmupRuns -> 240,
        exec.benchRuns -> 64,
        exec.independentSamples -> 6,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 6
      ) in {
        _.filter(_ % 2 == 0)
      }

      using(withPar(ranges(from, to, by))) curve("Range") in {
        _.filter(_ % 2 == 0)
      }
    }

    measure method "groupBy" config (
      exec.minWarmupRuns -> 80,
      exec.maxWarmupRuns -> 160,
      exec.benchRuns -> 36,
      exec.independentSamples -> 4,
      reports.regression.significance -> 1e-13,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 50000
      val to = 200000
      val by = 50000

      using(withPar(arrays(from, to, by))) curve("Array") in {
        _.groupBy(_ % 10)
      }

      using(withPar(arraybuffers(from, to, by))) curve("ArrayBuffer") in {
        _.groupBy(_ % 10)
      }

      using(withPar(vectors(from, to, by))) curve("Vector") in {
        _.groupBy(_ % 10)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 24,
        exec.independentSamples -> 4,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 4,
        exec.outliers.suspectPercent -> 50,
        exec.outliers.covMultiplier -> 2.0
      ) in {
        _.groupBy(_ % 10)
      }

      using(withPar(ranges(from, to, by))) curve("Range") in {
        _.groupBy(_ % 10)
      }
    }

    measure method "map" config (
      exec.minWarmupRuns -> 100,
      exec.maxWarmupRuns -> 200,
      exec.benchRuns -> 36,
      reports.regression.significance -> 1e-13,
      exec.independentSamples -> 4,
      reports.regression.noiseMagnitude -> 0.2
    ) in {
      val from = 100000
      val to = 400000
      val by = 100000

      using(withPar(arrays(from, to, by))) curve("Array") in {
        _.map(_ * 2)
      }

      using(withPar(arraybuffers(from, to, by))) curve("ArrayBuffer")  in {
        _.map(_ * 2)
      }
      
      using(withPar(vectors(from, to, by))) curve("Vector") in {
        _.map(_ * 2)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 48,
        exec.independentSamples -> 4,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 6,
        exec.noise.magnitude -> 1.0
      ) in {
        _.map(_ * 2)
      }

      using(withPar(ranges(from, to, by))) curve("Range") in {
        _.map(_ * 2)
      }
    }
  }
  */
}

















