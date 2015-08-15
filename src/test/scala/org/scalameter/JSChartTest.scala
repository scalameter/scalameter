package org.scalameter

import language.higherKinds

import collection._
import reporting._
import Key._
import collections._
import parallel.ParIterable



class JSChartTest extends Bench.Regression with Collections {

  def persistor = new persistence.SerializationPersistor()

  override def reporter = org.scalameter.Reporter.Composite(
    new RegressionReporter(
      RegressionReporter.Tester.OverlapIntervals(),
      RegressionReporter.Historian.ExponentialBackoff() ),
    HtmlReporter(true)
  )

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

      using(withPar(ranges(from, to, by))) curve("Range") in {
        _.map(_ * 2)
      }
    }
  }
}

















