package org.collperf
package collections



import collection._



class TraversableBenchmarks extends PerformanceTest.Regression {

  def persistor = new persistance.SerializationPersistor()

  /* data */

  def sizes(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = Gen.range("size")(from, to, by)

  def lists(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = for {
    size <- sizes(from, to, by)
  } yield (0 until size).toList

  def arrays(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = for {
    size <- sizes(from, to, by)
  } yield (0 until size).toArray

  def vectors(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = for {
    size <- sizes(from, to, by)
  } yield (0 until size).toVector

  def arraybuffers(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = for {
    size <- sizes(from, to, by)
  } yield mutable.ArrayBuffer(0 until size: _*)

  def ranges(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = for {
    size <- sizes(from, to, by)
  } yield 0 until size


  /* Large sequences */

  performance of "Traversable" in {

    measure method "foreach" configuration (
      Key.benchRuns -> 36,
      Key.independentSamples -> 9,
      Key.significance -> 1e-13
    ) in {
      val from = 1000000
      val to = 5000000
      val by = 1000000

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

      using(lists(from, to, by)) curve("List") configuration (
        Key.benchRuns -> 32,
        Key.independentSamples -> 4,
        Key.fullGC -> true,
        Key.frequency -> 5,
        Key.noiseMagnitude -> 1.0
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
  
    measure method "reduce" configuration (
      Key.benchRuns -> 36,
      Key.independentSamples -> 9,
      Key.significance -> 1e-13
    ) in {
      val from = 500000
      val to = 5000000
      val by = 1000000

      using(arrays(from, to, by)) curve("Array") in {
        _.reduce(_ + _)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer") in {
        _.reduce(_ + _)
      }

      using(vectors(from, to, by)) curve("Vector") in {
        _.reduce(_ + _)
      }

      using(lists(from, to, by)) curve("List") configuration (
        Key.benchRuns -> 20,
        Key.independentSamples -> 4,
        Key.fullGC -> true,
        Key.frequency -> 5,
        Key.noiseMagnitude -> 1.0
      ) in {
        _.reduce(_ + _)
      }

      using(ranges(from, to, by)) curve("Range") in {
        _.reduce(_ + _)
      }
    }
    
    measure method "filter" configuration (
      Key.benchRuns -> 36,
      Key.significance -> 1e-13,
      Key.independentSamples -> 9
    ) in {
      val from = 500000
      val to = 2500000
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

      using(lists(from, to, by)) curve("List") configuration (
        Key.benchRuns -> 48,
        Key.independentSamples -> 6,
        Key.fullGC -> true,
        Key.frequency -> 6,
        Key.noiseMagnitude -> 1.0
      ) in {
        _.filter(_ % 2 == 0)
      }

      using(ranges(from, to, by)) curve("Range") in {
        _.filter(_ % 2 == 0)
      }
    }

    measure method "groupBy" configuration (
      Key.benchRuns -> 36,
      Key.significance -> 1e-13,
      Key.independentSamples -> 9
    ) in {
      val from = 100000
      val to = 2000000
      val by = 400000

      using(arrays(from, to, by)) curve("Array") in {
        _.groupBy(_ % 10)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer") in {
        _.groupBy(_ % 10)
      }

      using(vectors(from, to, by)) curve("Vector") in {
        _.groupBy(_ % 10)
      }

      using(lists(from, to, by)) curve("List") configuration (
        Key.benchRuns -> 24,
        Key.independentSamples -> 4,
        Key.fullGC -> true,
        Key.frequency -> 4,
        Key.suspectPercent -> 50,
        Key.covMultiplier -> 2.0,
        Key.noiseMagnitude -> 1.0
      ) in {
        _.groupBy(_ % 10)
      }

      using(ranges(from, to, by)) curve("Range") in {
        _.groupBy(_ % 10)
      }
    }

    measure method "map" configuration (
      Key.benchRuns -> 36,
      Key.significance -> 1e-13,
      Key.independentSamples -> 9
    ) in {
      val from = 500000
      val to = 2000000
      val by = 400000

      using(arrays(from, to, by)) curve("Array") in {
        _.map(_ * 2)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer")  in {
        _.map(_ * 2)
      }
      
      using(vectors(from, to, by)) curve("Vector") in {
        _.map(_ * 2)
      }

      using(lists(from, to, by)) curve("List") configuration (
        Key.benchRuns -> 48,
        Key.independentSamples -> 6,
        Key.fullGC -> true,
        Key.frequency -> 6,
        Key.noiseMagnitude -> 1.0
      ) in {
        _.map(_ * 2)
      }

      using(ranges(from, to, by)) curve("Range") in {
        _.map(_ * 2)
      }
    }

    measure method "flatMap" configuration (
      Key.benchRuns -> 36,
      Key.significance -> 1e-13,
      Key.independentSamples -> 9
    ) in {
      val from = 250000
      val to = 1250000
      val by = 250000

      using(arrays(from, to, by)) curve("Array") in {
        _.flatMap(x => 0 until 2)
      }

      using(arraybuffers(from, to, by)) curve("ArrayBuffer") configuration (
      ) in {
        _.flatMap(x => 0 until 2)
      }
      
      using(vectors(from, to, by)) curve("Vector") in {
        _.flatMap(x => 0 until 2)
      }

      using(lists(from, to, by)) curve("List") configuration (
        Key.benchRuns -> 48,
        Key.independentSamples -> 6,
        Key.fullGC -> true,
        Key.frequency -> 6,
        Key.noiseMagnitude -> 1.0
      ) in {
        _.flatMap(x => 0 until 2)
      }

      using(ranges(from, to, by)) curve("Range") in {
        _.flatMap(x => 0 until 2)
      }
    }

  }

}

















