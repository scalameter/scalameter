package org.collperf



import collection._



class NewJvmMedianNoGcSeqTest extends SeqTesting with PerformanceTest with PerformanceTest.Reporter.Html {
  lazy val executor = new execution.JvmPerSetupExecutor(Aggregator.median, new Executor.Measurer.IgnoringGC)
}


class NewJvmMinNoGcSeqTest extends SeqTesting with PerformanceTest with PerformanceTest.Reporter.Html {
  lazy val executor = new execution.JvmPerSetupExecutor(Aggregator.min, new Executor.Measurer.IgnoringGC)
}


class NewJvmMinNoGcReinstSeqTest extends SeqTesting with PerformanceTest with PerformanceTest.Reporter.Html {
  lazy val executor = new execution.JvmPerSetupExecutor(Aggregator.min, new Executor.Measurer.IgnoringGC with Executor.Measurer.PeriodicReinstantiation {
    def frequency = 20
    def fullGC = true
  })
}


class NewJvmMedianNoGcFinderSeqTest extends SeqTesting with PerformanceTest with PerformanceTest.Reporter.Html {
  lazy val aggregator = Aggregator.median
  lazy val measurer = new Executor.Measurer.OptimalAllocation(new Executor.Measurer.IgnoringGC, aggregator)
  lazy val executor = new execution.JvmPerSetupExecutor(aggregator, measurer)
}


class NewJvmRegressionSeqTest extends SeqTesting with PerformanceTest.Executor.Regression with PerformanceTest.Reporter.Html


class RegressionSeqTest extends SeqTesting with PerformanceTest.Regression


abstract class SeqTesting extends PerformanceTest {

  def persistor = new persistance.SerializationPersistor()

  /* data */

  val largesizes = Gen.range("size")(500000, 5000000, 1000000)

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

      /*using(lists) curve("List") apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }

      using(mutablelists) curve("LinkedList") apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }*/
    }
  
    /*measure method "reduce" in {
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
    }*/

  }

}

















