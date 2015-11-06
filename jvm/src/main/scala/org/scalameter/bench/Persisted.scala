package org.scalameter
package bench

import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.Pickler

/**
 * Benchmark that runs snippets in separate JVMs.
 *  It persists results in the gzipped JSON format with appropriate reporter.
 */
abstract class Persisted[U: Pickler: PrettyPrinter] extends Bench[U] {
  def warmer: Warmer = new Warmer.Default

  def aggregator: Aggregator[U]

  def executor: Executor[U] = new execution.SeparateJvmsExecutor[U](
    warmer,
    aggregator,
    measurer)

  def persistor: Persistor = new persistence.GZIPJSONSerializationPersistor
}

/**
 * A base for benchmarks generating a more detailed (regression) report,
 *  potentially online.
 */
abstract class HTMLReport extends Persisted[Double] {
  import reporting._
  def aggregator: Aggregator[Double] = Aggregator.average
  def measurer: Measurer[Double] = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation[Double] with Measurer.OutlierElimination[Double] with Measurer.RelativeNoise {
    def numeric: Numeric[Double] = implicitly[Numeric[Double]]
  }
  def tester: RegressionReporter.Tester
  def historian: RegressionReporter.Historian
  def online: Boolean
  def reporter: Reporter[Double] = Reporter.Composite(
    new RegressionReporter(tester, historian),
    HtmlReporter(!online))
}

/**
 * Runs in separate JVM, performs regression tests and
 *  prepares HTML document for online hosting.
 */
abstract class OnlineRegressionReport extends HTMLReport {
  import reporting._
  def tester: RegressionReporter.Tester =
    RegressionReporter.Tester.OverlapIntervals()
  def historian: RegressionReporter.Historian =
    RegressionReporter.Historian.ExponentialBackoff()
  def online = true
}

/**
 * Runs in separate JVM, performs regression tests and
 *  prepares an offline HTML document.
 */
abstract class OfflineRegressionReport extends HTMLReport {
  import reporting._
  def tester: RegressionReporter.Tester =
    RegressionReporter.Tester.OverlapIntervals()
  def historian: RegressionReporter.Historian =
    RegressionReporter.Historian.ExponentialBackoff()
  def online = false
}

/**
 * Runs in separate JVM and prepares an offline HTML document.
 *  Does not regression testing.
 */
abstract class OfflineReport extends HTMLReport {
  import reporting._
  def tester: RegressionReporter.Tester =
    RegressionReporter.Tester.Accepter()
  def historian: RegressionReporter.Historian =
    RegressionReporter.Historian.ExponentialBackoff()
  def online = false
}
