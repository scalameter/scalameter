package org.scalameter
package bench

import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.Pickler

/**
 * Benchmark that runs snippets in the same JVM.
 *  Reports result into the console.
 */
abstract class Local[U: Pickler] extends Bench[U] {
  def warmer: Warmer = new Warmer.Default

  def aggregator: Aggregator[U]

  def executor: Executor[U] = new execution.LocalExecutor(
    warmer,
    aggregator,
    measurer)

  def persistor: Persistor = Persistor.None

  def reporter: Reporter[U] = new reporting.LoggingReporter
}

/**
 * Quick benchmark runs snippets in the same JVM.
 *  Reports execution time into the console.
 */
abstract class LocalTime extends Local[Double] {
  def measurer: Measurer[Double] = new Measurer.Default
  def aggregator: Aggregator[Double] = Aggregator.min
}