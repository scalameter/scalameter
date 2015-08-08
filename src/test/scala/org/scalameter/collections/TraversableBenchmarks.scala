package org.scalameter
package collections



import collection._
import Key._



class TraversableBenchmarks extends Bench.Regression with Collections {

  def persistor = new persistence.SerializationPersistor()

  /* traversable collections */

  performance of "Traversable" in {

    measure method "foreach" config (
      exec.minWarmupRuns -> 50,
      exec.maxWarmupRuns -> 150,
      exec.benchRuns -> 36,
      exec.independentSamples -> 9,
      reports.regression.significance -> 1e-13
    ) in {
      val from = 1000000
      val to = 5000000
      val by = 2000000

      using(arrays(from, to, by)) curve("Array") in { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer") in { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }
      
      using(vectors(from, to, by)) curve("Vector") in { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 32,
        exec.independentSamples -> 4,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 5,
        exec.noise.magnitude -> 1.0
      ) in { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }

      using(ranges(from, to, by)) curve("Range") in { xs =>
        var sum = 0
        xs.foreach(sum += _)
        xs.foreach(sum += _)
      }
    }
  
    measure method "reduce" config (
      exec.minWarmupRuns -> 20,
      exec.benchRuns -> 36,
      exec.independentSamples -> 9,
      reports.regression.significance -> 1e-13
    ) in {
      val from = 500000
      val to = 1500000
      val by = 500000

      using(arrays(from, to, by)) curve("Array") in {
        _.reduce(_ + _)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer") in {
        _.reduce(_ + _)
      }

      using(vectors(from, to, by)) curve("Vector") in {
        _.reduce(_ + _)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 20,
        exec.independentSamples -> 4,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 5,
        exec.noise.magnitude -> 1.0
      ) in {
        _.reduce(_ + _)
      }

      using(ranges(from, to, by)) curve("Range") in {
        _.reduce(_ + _)
      }
    }
    
    measure method "filter" config (
      exec.benchRuns -> 36,
      reports.regression.significance -> 1e-13,
      exec.independentSamples -> 9
    ) in {
      val from = 500000
      val to = 2000000
      val by = 500000

      using(arrays(from, to, by)) curve("Array") in {
        _.filter(_ % 2 == 0)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer")  in {
        _.filter(_ % 2 == 0)
      }
      
      using(vectors(from, to, by)) curve("Vector") in {
        _.filter(_ % 2 == 0)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 48,
        exec.independentSamples -> 6,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 6,
        exec.noise.magnitude -> 1.0
      ) in {
        _.filter(_ % 2 == 0)
      }

      using(ranges(from, to, by)) curve("Range") config (
        exec.minWarmupRuns -> 25
      ) in {
        _.filter(_ % 2 == 0)
      }
    }

    measure method "groupBy" config (
      exec.benchRuns -> 36,
      reports.regression.significance -> 1e-13,
      exec.independentSamples -> 9
    ) in {
      val from = 100000
      val to = 600000
      val by = 200000

      using(arrays(from, to, by)) curve("Array") in {
        _.groupBy(_ % 10)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer") in {
        _.groupBy(_ % 10)
      }

      using(vectors(from, to, by)) curve("Vector") in {
        _.groupBy(_ % 10)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 24,
        exec.independentSamples -> 4,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 4,
        exec.outliers.suspectPercent -> 50,
        exec.outliers.covMultiplier -> 2.0,
        exec.noise.magnitude -> 1.0
      ) in {
        _.groupBy(_ % 10)
      }

      using(ranges(from, to, by)) curve("Range") in {
        _.groupBy(_ % 10)
      }
    }

    measure method "map" config (
      exec.benchRuns -> 36,
      reports.regression.significance -> 1e-13,
      exec.independentSamples -> 9
    ) in {
      val from = 500000
      val to = 2000000
      val by = 600000

      using(arrays(from, to, by)) curve("Array") in {
        _.map(_ * 2)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer")  in {
        _.map(_ * 2)
      }
      
      using(vectors(from, to, by)) curve("Vector") in {
        _.map(_ * 2)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 48,
        exec.independentSamples -> 6,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 6,
        exec.noise.magnitude -> 1.0
      ) in {
        _.map(_ * 2)
      }

      using(ranges(from, to, by)) curve("Range") in {
        _.map(_ * 2)
      }
    }

    measure method "flatMap" config (
      exec.benchRuns -> 36,
      reports.regression.significance -> 1e-13,
      exec.independentSamples -> 9
    ) in {
      val from = 250000
      val to = 750000
      val by = 250000

      using(arrays(from, to, by)) curve("Array") in {
        _.flatMap(x => 0 until 2)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer") config (
      ) in {
        _.flatMap(x => 0 until 2)
      }
      
      using(vectors(from, to, by)) curve("Vector") in {
        _.flatMap(x => 0 until 2)
      }

      using(lists(from, to, by)) curve("List") config (
        exec.benchRuns -> 48,
        exec.independentSamples -> 6,
        exec.reinstantiation.fullGC -> true,
        exec.reinstantiation.frequency -> 6,
        exec.noise.magnitude -> 1.0
      ) in {
        _.flatMap(x => 0 until 2)
      }

      using(ranges(from, to, by)) curve("Range") in {
        _.flatMap(x => 0 until 2)
      }
    }

  }

}

















