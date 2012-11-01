package org.collperf
package collections



import collection._



class SeqBenchmarks extends PerformanceTest.Regression with Collections {

  def persistor = new persistance.SerializationPersistor()

  /* tests */

  performance of "Seq" in {

    // measure method "apply" configuration (
    //   Key.benchRuns -> 36,
    //   Key.independentSamples -> 9,
    //   Key.significance -> 1e-13
    // ) in {
    //   val from = 1000000
    //   val to = 5000000
    //   val by = 1000000
    //   var sideeffect = 0

    //   using(arrays(from, to, by)) curve("Array") in { xs =>
    //     var i = 0
    //     var sum = 0
    //     val len = xs.length
    //     val until = len * 3
    //     while (i < until) {
    //       sum += xs.apply(i % len)
    //       i += 1
    //     }
    //     sideeffect = sum
    //   }

    //   using(arraybuffers(from, to, by)) curve("ArrayBuffer") in { xs =>
    //     var i = 0
    //     var sum = 0
    //     val len = xs.length
    //     val until = len * 3
    //     while (i < until) {
    //       sum += xs.apply(i % len)
    //       i += 1
    //     }
    //     sideeffect = sum
    //   }

    //   using(vectors(from, to, by)) curve("Vector") in { xs =>
    //     var i = 0
    //     var sum = 0
    //     val len = xs.length
    //     val until = len * 3
    //     while (i < until) {
    //       sum += xs.apply(i % len)
    //       i += 1
    //     }
    //     sideeffect = sum
    //   }

    //   using(ranges(from, to, by)) curve("Range") in { xs =>
    //     var i = 0
    //     var sum = 0
    //     val len = xs.length
    //     val until = len * 3
    //     while (i < until) {
    //       sum += xs.apply(i % len)
    //       i += 1
    //     }
    //     sideeffect = sum
    //   }

    // }

    // updates

    // measure method "update" configuration (
    //   Key.benchRuns -> 36,
    //   Key.independentSamples -> 9,
    //   Key.significance -> 1e-13
    // ) in {
    //   val from = 1000000
    //   val to = 5000000
    //   val by = 1000000
    //   var sideeffect = 0

    //   using(arrays(from, to, by)) curve("Array") in { xs =>
    //     var i = 0
    //     var sum = 0
    //     val len = xs.length
    //     val until = len * 3
    //     while (i < until) {
    //       xs.update(i % len, i)
    //       i += 1
    //     }
    //     sideeffect = sum
    //   }

    //   using(arraybuffers(from, to, by)) curve("ArrayBuffer") in { xs =>
    //     var i = 0
    //     var sum = 0
    //     val len = xs.length
    //     val until = len * 3
    //     while (i < until) {
    //       xs.update(i % len, i)
    //       i += 1
    //     }
    //     sideeffect = sum
    //   }

    // }

    // measure method "append" configuration (
    //   Key.benchRuns -> 36,
    //   Key.independentSamples -> 9,
    //   Key.significance -> 1e-13
    // ) in {
    //   val from = 200000
    //   val to = 2200000
    //   val by = 400000

    //   using(sizes(from, to, by)) curve("Vector") configuration (
    //     Key.benchRuns -> 32,
    //     Key.independentSamples -> 4,
    //     Key.suspectPercent -> 66,
    //     Key.covMultiplier -> 1.4,
    //     Key.noiseMagnitude -> 1.0
    //   ) in { len =>
    //     var i = 0
    //     var vector = Vector.empty[Int]
    //     while (i < len) {
    //       vector = vector :+ i
    //       i += 1
    //     }
    //   }

    // }

    measure method "prepend" configuration (
      Key.benchRuns -> 36,
      Key.independentSamples -> 9,
      Key.significance -> 1e-13
    ) in {
      val from = 200000
      val to = 2200000
      val by = 400000

      // using(sizes(from, to, by)) curve("Vector") configuration (
      //   Key.benchRuns -> 32,
      //   Key.independentSamples -> 4,
      //   Key.suspectPercent -> 66,
      //   Key.covMultiplier -> 1.4,
      //   Key.noiseMagnitude -> 1.0
      // ) in { len =>
      //   var i = 0
      //   var vector = Vector.empty[Int]
      //   while (i < len) {
      //     vector = i +: vector
      //     i += 1
      //   }
      // }

      using(sizes(from, to, by)) curve("List") configuration (
      ) in { len =>
        var i = 0
        var list = List.empty[Int]
        while (i < len) {
          list = i :: list
          i += 1
        }
      }

    }

    // sorting

  }

}

















