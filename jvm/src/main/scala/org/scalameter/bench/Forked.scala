package org.scalameter
package bench

import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.Pickler

/**
 * Benchmark that runs snippets in separate JVMs.
 *  Reports result into the console.
 */
abstract class Forked[U: Pickler: PrettyPrinter] extends Bench[U] {
  def warmer: Warmer = new Warmer.Default

  def aggregator: Aggregator[U]

  def executor: Executor[U] = new execution.SeparateJvmsExecutor(
    warmer,
    aggregator,
    measurer)

  def persistor: Persistor = Persistor.None

  def reporter: Reporter[U] = new reporting.LoggingReporter
}

/**
 * A more reliable benchmark run in a separate JVM.
 *  Reports execution time into the console.
 */
abstract class ForkedTime extends Forked[Double] {
  def aggregator: Aggregator[Double] = Aggregator.min
  def measurer: Measurer[Double] = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation[Double] {
    override val defaultFrequency = 12
    override val defaultFullGC = true
  }
}