package org.scalameter
package bench

import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.Pickler

@deprecated(
  "This performance test is now deprecated, " +
    "please use `OnlineRegressionReport` instead.",
  "0.5")
abstract class Regression extends Bench[Double] {
  import reporting._
  def warmer: Warmer = Warmer.Default()
  def aggregator: Aggregator[Double] = Aggregator.average
  def measurer: Measurer[Double] = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation[Double] with Measurer.OutlierElimination[Double] with Measurer.RelativeNoise {
    def numeric: Numeric[Double] = implicitly[Numeric[Double]]
  }
  def executor: Executor[Double] =
    new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
  def reporter: Reporter[Double] = Reporter.Composite(
    new RegressionReporter(
      RegressionReporter.Tester.OverlapIntervals(),
      RegressionReporter.Historian.ExponentialBackoff()),
    HtmlReporter(false))
}