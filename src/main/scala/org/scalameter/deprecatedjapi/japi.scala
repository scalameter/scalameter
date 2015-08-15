package org.scalameter.deprecatedjapi



import org.scalameter.reporting
import org.scalameter.reporting.ChartReporter
import org.scalameter.picklers.Implicits._



abstract class Executor[T] {
  def get: org.scalameter.Executor[T]
}


abstract class Aggregator[T] {
  def get: org.scalameter.Aggregator[T]
}


abstract class Measurer[T] {
  def get: org.scalameter.Executor.Measurer[T]
}


abstract class Persistor {
  def get: org.scalameter.Persistor
}


abstract class Reporter[T] {
  def get: org.scalameter.Reporter[T]
}


abstract class RegressionReporterTester {
  def get: org.scalameter.reporting.RegressionReporter.Tester
}


abstract class RegressionReporterHistorian {
  def get: org.scalameter.reporting.RegressionReporter.Historian
}


class OverlapIntervalsTester extends RegressionReporterTester {
  def get = org.scalameter.reporting.RegressionReporter.Tester.OverlapIntervals()
}


class AccepterTester extends RegressionReporterTester {
  def get = org.scalameter.reporting.RegressionReporter.Tester.Accepter()
}


class ANOVATester extends RegressionReporterTester {
  def get = org.scalameter.reporting.RegressionReporter.Tester.ANOVA()
}


class ExponentialBackoffHistorian extends RegressionReporterHistorian {
  def get = org.scalameter.reporting.RegressionReporter.Historian.ExponentialBackoff()
}


class CompleteHistorian extends RegressionReporterHistorian {
  def get = org.scalameter.reporting.RegressionReporter.Historian.Complete()
}


class LocalExecutor(w: Warmer, a: Aggregator[Double], m: Measurer[Double]) extends Executor[Double] {
  def get: org.scalameter.Executor[Double] = new org.scalameter.execution.LocalExecutor(w.get, a.get, m.get)
}


class SeparateJvmsExecutor(w: Warmer, a: Aggregator[Double], m: Measurer[Double]) extends Executor[Double] {
  def get = org.scalameter.execution.SeparateJvmsExecutor(w.get, a.get, m.get)
}


abstract class Warmer {
  def get: org.scalameter.Executor.Warmer
}


class MinAggregator[T: Ordering] extends Aggregator[T] {
  def get = org.scalameter.Aggregator.min
}


class AverageAggregator extends Aggregator[Double] {
  def get = org.scalameter.Aggregator.average
}


class MaxAggregator[T: Ordering] extends Aggregator[T] {
  def get = org.scalameter.Aggregator.max
}


class MedianAggregator[T: Ordering] extends Aggregator[T] {
  def get = org.scalameter.Aggregator.median
}


class DefaultMeasurer extends Measurer[Double] {
  def get = new org.scalameter.Executor.Measurer.Default
}


class DefaultWithNanosMeasurer extends Measurer[Double] {
  def get = org.scalameter.Executor.Measurer.Default.withNanos()
}


class TimeWithIgnoringGCMeasurer extends Measurer[Double] {
  def get = new org.scalameter.Executor.Measurer.IgnoringGC
}


class TimeWithIgnoringGCWithOutlierEliminationMeasurer extends Measurer[Double] {
  def get = new org.scalameter.Executor.Measurer.IgnoringGC
    with org.scalameter.Executor.Measurer.OutlierElimination[Double] {
    def numeric: Numeric[Double] = implicitly[Numeric[Double]]
  }
}


class TimeWithIngoringGCWithPeriodicReinstantiationWithOutlierEliminationMeasurer extends Measurer[Double] {
  def get = new org.scalameter.Executor.Measurer.IgnoringGC
    with org.scalameter.Executor.Measurer.PeriodicReinstantiation[Double]
    with org.scalameter.Executor.Measurer.OutlierElimination[Double] {
    def numeric: Numeric[Double] = implicitly[Numeric[Double]]
  }
}


class MemoryFootprintMeasurer extends Measurer[Double] {
  def get = new org.scalameter.Executor.Measurer.MemoryFootprint
}


class NonePersistor extends Persistor {
  def get: org.scalameter.Persistor = org.scalameter.Persistor.None
}


class SerializationPersistor extends Persistor {
  def get = new org.scalameter.persistence.SerializationPersistor
}


class JSONSerializationPersistor extends Persistor {
  def get = new org.scalameter.persistence.JSONSerializationPersistor
}


class GZIPJSONSerializationPersistor extends Persistor {
  def get = new org.scalameter.persistence.GZIPJSONSerializationPersistor
}


class LoggingReporter[T] extends Reporter[T] {
  def get: org.scalameter.Reporter[T] = org.scalameter.reporting.LoggingReporter[T]()
}


class RegressionReporter(tester: RegressionReporterTester, historian: RegressionReporterHistorian) extends Reporter[Double] {
  def get = org.scalameter.reporting.RegressionReporter(tester.get, historian.get)
}
