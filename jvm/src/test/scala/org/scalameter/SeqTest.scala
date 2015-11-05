package org.scalameter



import scala.collection._



class RegressionSeqTest extends Bench.OnlineRegressionReport with SeqTesting


trait SeqTesting { this: Bench[Double] =>

  /* data */

  def largesizes(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = Gen.range("size")(from, to, by)

  def lists(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = for {
    size <- largesizes(from, to, by)
  } yield (0 until size).toList

  def arrays(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = for {
    size <- largesizes(from, to, by)
  } yield (0 until size).toArray

  def vectors(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = for {
    size <- largesizes(from, to, by)
  } yield (0 until size).toVector

  def arraybuffers(from: Int = 500000, to: Int = 5000000, by: Int = 1000000) = for {
    size <- largesizes(from, to, by)
  } yield mutable.ArrayBuffer(0 until size: _*)


  /* Large sequences */

  performance of "Large-Seq" in {

    // measure method "foreach" config (
    //   Key.benchRuns -> 36,
    //   Key.independentSamples -> 9,
    //   Key.significance -> 1e-13
    // ) in {
    //   val from = 1000000

    //   using(arrays(from)) curve("Array") in { xs =>
    //     var sum = 0
    //     xs.foreach(sum += _)
    //   }

    //   using(arraybuffers(from)) curve("ArrayBuffer") in { xs =>
    //     var sum = 0
    //     xs.foreach(sum += _)
    //   }
      
    //   using(vectors(from)) curve("Vector") in { xs =>
    //     var sum = 0
    //     xs.foreach(sum += _)
    //   }

    //   using(lists(from)) curve("List") config (
    //     Key.benchRuns -> 32,
    //     Key.independentSamples -> 4,
    //     Key.fullGC -> true,
    //     Key.frequency -> 5,
    //     Key.noiseMagnitude -> 1.0
    //   ) in { xs =>
    //     var sum = 0
    //     xs.foreach(sum += _)
    //   }
    // }
  
    // measure method "reduce" config (
    //   Key.benchRuns -> 36,
    //   Key.independentSamples -> 9,
    //   Key.significance -> 1e-13
    // ) in {
    //   val from = 500000
    //   val to = 3500000
    //   val by = 700000

    //   using(arrays()) curve("Array") in {
    //     _.reduce(_ + _)
    //   }

    //   using(arraybuffers()) curve("ArrayBuffer") in {
    //     _.reduce(_ + _)
    //   }

    //   using(vectors()) curve("Vector") in {
    //     _.reduce(_ + _)
    //   }

    //   using(lists()) curve("List") config (
    //     Key.benchRuns -> 20,
    //     Key.independentSamples -> 4,
    //     Key.fullGC -> true,
    //     Key.frequency -> 5,
    //     Key.noiseMagnitude -> 1.0
    //   ) in {
    //     _.reduce(_ + _)
    //   }
    // }
    
    // measure method "filter" config (
    //   Key.benchRuns -> 36,
    //   Key.significance -> 1e-13,
    //   Key.independentSamples -> 9
    // ) in {
    //   val from = 500000
    //   val to = 2500000
    //   val by = 500000

    //   using(arrays(from, to, by)) curve("Array") in {
    //     _.filter(_ % 2 == 0)
    //   }

    //   using(arraybuffers(from, to, by)) curve("ArrayBuffer")  in {
    //     _.filter(_ % 2 == 0)
    //   }
      
    //   using(vectors(from, to, by)) curve("Vector") in {
    //     _.filter(_ % 2 == 0)
    //   }

    //   using(lists(from, to, by)) curve("List") config (
    //     Key.benchRuns -> 48,
    //     Key.independentSamples -> 6,
    //     Key.fullGC -> true,
    //     Key.frequency -> 6,
    //     Key.noiseMagnitude -> 1.0
    //   ) in {
    //     _.filter(_ % 2 == 0)
    //   }
    // }

    // measure method "groupBy" config (
    //   Key.benchRuns -> 36,
    //   Key.significance -> 1e-13,
    //   Key.independentSamples -> 9
    // ) in {
    //   val from = 100000
    //   val to = 2000000
    //   val by = 400000

    //   using(arrays(from, to, by)) curve("Array") in {
    //     _.groupBy(_ % 10)
    //   }

    //   using(arraybuffers(from, to, by)) curve("ArrayBuffer") in {
    //     _.groupBy(_ % 10)
    //   }

    //   using(vectors(from, to, by)) curve("Vector") in {
    //     _.groupBy(_ % 10)
    //   }

    //   using(lists(from, to, by)) curve("List") config (
    //     Key.benchRuns -> 24,
    //     Key.independentSamples -> 4,
    //     Key.fullGC -> true,
    //     Key.frequency -> 4,
    //     Key.suspectPercent -> 50,
    //     Key.covMultiplier -> 2.0,
    //     Key.noiseMagnitude -> 1.0
    //   ) in {
    //     _.groupBy(_ % 10)
    //   }
    // }

  }

}

















