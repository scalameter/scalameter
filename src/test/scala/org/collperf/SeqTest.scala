package org.collperf



import collection._



class LocalSeqTest extends SeqTesting with PerformanceTest.Executor.LocalMin with PerformanceTest.Reporter.Html


class NewJvmSeqTest extends SeqTesting with PerformanceTest.Executor.NewJvmMedian with PerformanceTest.Reporter.Html


class NewJvmMedianNoGcSeqTest extends SeqTesting with PerformanceTest with PerformanceTest.Reporter.Html {

  lazy val executor = new execution.NewJvmExecutor(Aggregator.median, new Executor.Measurer.IgnoringGC)

}


class NewJvmMinNoGcSeqTest extends SeqTesting with PerformanceTest with PerformanceTest.Reporter.Html {

  lazy val executor = new execution.NewJvmExecutor(Aggregator.min, new Executor.Measurer.IgnoringGC)

}


class NewJvmMinNoGcReinstSeqTest extends SeqTesting with PerformanceTest with PerformanceTest.Reporter.Html {

  lazy val executor = new execution.NewJvmExecutor(Aggregator.min, new Executor.Measurer.IgnoringGC with Executor.Measurer.Reinstantiation {
    def frequency = 20
    def fullGC = true
  })

}


abstract class SeqTesting extends PerformanceTest {

  val largesizes = Gen.range("size")(500000, 5000000, 250000)

  val lists = for {
    size <- largesizes
  } yield (0 until size).toList

  val arrays = for {
    size <- largesizes
  } yield (0 until size).toArray

  val vectors = for {
    size <- largesizes
  } yield (0 until size).toVector

  val mutablelists = for {
    size <- largesizes
  } yield mutable.LinkedList(0 until size: _*)

  val arraybuffers = for {
    size <- largesizes
  } yield mutable.ArrayBuffer(0 until size: _*)


  /* Large sequences */

  performance of "Large-Seq" in {

    measure method "foreach" in {
      using(arrays) curve("Array") apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }

      using(arraybuffers) curve("ArrayBuffer") apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }
      
      using(vectors) curve("Vector") apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }

      using(lists) curve("List") apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }

      using(mutablelists) curve("LinkedList") apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }
    }
  
    measure method "reduce" in {
      using(arrays) curve("Array") apply {
        _.reduce(_ + _)
      }

      using(arraybuffers) curve("ArrayBuffer") apply {
        _.reduce(_ + _)
      }

      using(vectors) curve("Vector") apply {
        _.reduce(_ + _)
      }

      using(lists) curve("List") apply {
        _.reduce(_ + _)
      }

      using(mutablelists) curve("LinkedList") apply {
        _.reduce(_ + _)
      }
    }
    
    measure method "filter" in {
      using(arrays) curve("Array") apply {
        _.filter(_ % 2 == 0)
      }

      using(arraybuffers) curve("ArrayBuffer") apply {
        _.filter(_ % 2 == 0)
      }
      
      using(vectors) curve("Vector") apply {
        _.filter(_ % 2 == 0)
      }

      using(lists) curve("List") apply {
        _.filter(_ % 2 == 0)
      }

      using(mutablelists) curve("LinkedList") apply {
        _.filter(_ % 2 == 0)
      }
    }

    measure method "groupBy" in {
      using(arrays) curve("Array") apply {
        _.groupBy(_ % 10)
      }

      using(arraybuffers) curve("ArrayBuffer") apply {
        _.groupBy(_ % 10)
      }

      using(vectors) curve("Vector") apply {
        _.groupBy(_ % 10)
      }

      using(lists) curve("List") apply {
        _.groupBy(_ % 10)
      }

      using(mutablelists) curve("LinkedList") apply {
        _.groupBy(_ % 10)
      }
    }

  }

}

















