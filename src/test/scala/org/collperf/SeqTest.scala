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

  def largesizes(from: Int = 500000) = Gen.range("size")(from, from + 5000000, 1000000)

  def lists(from: Int = 500000) = for {
    size <- largesizes(from)
  } yield (0 until size).toList

  def arrays(from: Int = 500000) = for {
    size <- largesizes(from)
  } yield (0 until size).toArray

  def vectors(from: Int = 500000) = for {
    size <- largesizes(from)
  } yield (0 until size).toVector

  def arraybuffers(from: Int = 500000) = for {
    size <- largesizes(from)
  } yield mutable.ArrayBuffer(0 until size: _*)


  /* Large sequences */

  performance of "Large-Seq" in {

    measure method "foreach" in {
      /*using(arrays(1000000)) curve("Array") configuration (
        Key.significance -> 1e-4
      ) apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }

      using(arraybuffers(1000000)) curve("ArrayBuffer") configuration (
        Key.significance -> 1e-4
      ) apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }
      
      using(vectors(1000000)) curve("Vector")  configuration (
        Key.significance -> 1e-4
      ) apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }

      using(lists(1000000)) curve("List")  configuration (
        Key.significance -> 1e-4
      ) apply { xs =>
        var sum = 0
        xs.foreach(sum += _)
      }*/
    }
  
    measure method "reduce" in {
      using(arrays()) curve("Array") configuration (
        Key.significance -> 1e-7
      ) apply {
        _.reduce(_ + _)
      }

      /*using(arraybuffers()) curve("ArrayBuffer") configuration (
        Key.significance -> 1e-4
      ) apply {
        _.reduce(_ + _)
      }

      using(vectors()) curve("Vector") configuration (
        Key.significance -> 1e-4
      ) apply {
        _.reduce(_ + _)
      }

      using(lists()) curve("List") configuration (
        Key.significance -> 1e-4
      ) apply {
        _.reduce(_ + _)
      }*/
    }
    
    measure method "filter" in {
      /*using(arrays) curve("Array") apply {
        _.filter(_ % 2 == 0)
      }*/

      /*using(arraybuffers) curve("ArrayBuffer") apply {
        _.filter(_ % 2 == 0)
      }
      
      using(vectors) curve("Vector") apply {
        _.filter(_ % 2 == 0)
      }

      using(lists) curve("List") apply {
        _.filter(_ % 2 == 0)
      }*/
    }

    /*measure method "groupBy" in {
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
    }*/

  }

}

















