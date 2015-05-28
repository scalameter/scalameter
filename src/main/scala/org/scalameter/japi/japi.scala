package org.scalameter.japi



import org.scalameter.reporting
import org.scalameter.reporting.ChartReporter



abstract class Executor {
  def get: org.scalameter.Executor
}


abstract class Aggregator {
  def get: org.scalameter.Aggregator
}


abstract class Measurer {
  def get: org.scalameter.Executor.Measurer
}


abstract class Persistor {
  def get: org.scalameter.Persistor
}


abstract class Reporter {
  def get: org.scalameter.Reporter
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


class LocalExecutor(w: Warmer, a: Aggregator, m: Measurer) extends Executor {
  def get: org.scalameter.Executor = new org.scalameter.execution.LocalExecutor(w.get, a.get, m.get)
}


class SeparateJvmsExecutor(w: Warmer, a: Aggregator, m: Measurer) extends Executor {
  def get = org.scalameter.execution.SeparateJvmsExecutor(w.get, a.get, m.get)
}


abstract class Warmer {
  def get: org.scalameter.Executor.Warmer
}


class MinAggregator extends Aggregator {
  def get = org.scalameter.Aggregator.min
}


class AverageAggregator extends Aggregator {
  def get = org.scalameter.Aggregator.average
}


class MaxAggregator extends Aggregator {
  def get = org.scalameter.Aggregator.max
}


class MedianAggregator extends Aggregator {
  def get = org.scalameter.Aggregator.median
}


class DefaultMeasurer extends Measurer {
  def get = new org.scalameter.Executor.Measurer.Default
}


class DefaultWithNanosMeasurer extends Measurer {
  def get = org.scalameter.Executor.Measurer.Default.withNanos()
}


class TimeWithIgnoringGCMeasurer extends Measurer {
  def get = new org.scalameter.Executor.Measurer.IgnoringGC
}


class TimeWithIgnoringGCWithOutlierEliminationMeasurer extends Measurer {
  def get = new org.scalameter.Executor.Measurer.IgnoringGC with org.scalameter.Executor.Measurer.OutlierElimination
}


class TimeWithIngoringGCWithPeriodicReinstantiationWithOutlierEliminationMeasurer extends Measurer {
  def get = new org.scalameter.Executor.Measurer.IgnoringGC with org.scalameter.Executor.Measurer.PeriodicReinstantiation with org.scalameter.Executor.Measurer.OutlierElimination
}


class MemoryFootprintMeasurer extends Measurer {
  def get = new org.scalameter.Executor.Measurer.MemoryFootprint
}


class NonePersistor extends Persistor {
  def get: org.scalameter.Persistor = org.scalameter.Persistor.None
}


class SerializationPersistor extends Persistor {
  def get = new org.scalameter.persistence.SerializationPersistor
}


class LoggingReporter extends Reporter {
  def get: org.scalameter.Reporter = org.scalameter.reporting.LoggingReporter()
}


class RegressionReporter(tester: RegressionReporterTester, historian: RegressionReporterHistorian) extends Reporter {
  def get = org.scalameter.reporting.RegressionReporter(tester.get, historian.get)
}
