package org.scalameter.javaApi

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

class OverlapIntervals extends RegressionReporterTester{
  def get = org.scalameter.reporting.RegressionReporter.Tester.OverlapIntervals()
}
class Accepter extends RegressionReporterTester{
  def get = org.scalameter.reporting.RegressionReporter.Tester.Accepter()
}

class ANOVA extends RegressionReporterTester{
  def get = org.scalameter.reporting.RegressionReporter.Tester.ANOVA()
}
class ExponentialBackoff extends RegressionReporterHistorian{
  def get = org.scalameter.reporting.RegressionReporter.Historian.ExponentialBackoff()
}
class Complete extends RegressionReporterHistorian{
  def get = org.scalameter.reporting.RegressionReporter.Historian.Complete()
}

class LocalExecutor(w: org.scalameter.Executor.Warmer, a: Aggregator, m: Measurer) extends Executor {
  def get: org.scalameter.Executor = new org.scalameter.execution.LocalExecutor(w, a.get, m.get)
}
class SeparateJvmsExecutor(w: org.scalameter.Executor.Warmer, a: Aggregator, m: Measurer) extends Executor {
  def get = org.scalameter.execution.SeparateJvmsExecutor(w, a.get, m.get)
}

class Warmer {
  def Default: org.scalameter.Executor.Warmer = org.scalameter.Executor.Warmer.Default()
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
class TimeWithIgnoringGC extends Measurer {
  def get = new org.scalameter.Executor.Measurer.IgnoringGC
}
class TimeWithIgnoringGCWithOutlierElimination extends Measurer {
  def get = new org.scalameter.Executor.Measurer.IgnoringGC with org.scalameter.Executor.Measurer.OutlierElimination
}
class TimeWithIngoringGCWithPeriodicReinstantiationWithOutlierElimination extends Measurer {
  def get = new org.scalameter.Executor.Measurer.IgnoringGC with org.scalameter.Executor.Measurer.PeriodicReinstantiation with org.scalameter.Executor.Measurer.OutlierElimination
}
class MemoryFootprint extends Measurer {
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
//for some reasons I could not test this, compiler allways tells me that it cannot find symbol when instantiated...
/*
class XYLine(fileNamePrefix: String, wdt: Int, hgt: Int) extends Reporter {
  def get: org.scalameter.Reporter = ChartReporter(new org.scalameter.reporting.ChartReporter.ChartFactory.XYLine, fileNamePrefix, wdt, hgt)
}*/
class CompositeReporter(tester: RegressionReporterTester, historian: RegressionReporterHistorian, online: Boolean) extends Reporter {
  def get = org.scalameter.Reporter.Composite(
      new org.scalameter.reporting.RegressionReporter(tester.get, historian.get),
      org.scalameter.reporting.HtmlReporter(!online))
}

class RegressionReporter(tester: RegressionReporterTester, historian: RegressionReporterHistorian) extends Reporter {
  def get = org.scalameter.reporting.RegressionReporter(tester.get, historian.get)
}
