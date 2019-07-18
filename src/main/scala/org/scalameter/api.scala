package org.scalameter

/** Import the contents of this singleton object to obtain access to most abstractions
 *  in the ScalaMeter API.
 *
 *  Note that some definitions might shadow others, so if you import the contents of
 *  this object, you should not import the contents of other packages directly.
 *
 *  This object contains:
 *  - basic datatypes and singleton objects for writing tests, such as `PerformanceTest`
 *  - all the context map keys
 *  - contents of the `execution` package
 *  - contents of the `reporting` package
 *  - contents of the `persistence` package
 *  - the `Executor.Measurer` object
 *  - the `RegressionReporter.Tester` object
 *  - the `RegressionReporter.Historian` object
 *  - the `ChartReporter.ChartFactory` object
 *  - the `HtmlReporter.Renderer` object
 *  - and much more...
 */
object api extends Keys {

  type Gen[T] = org.scalameter.Gen[T]
  val Gen = org.scalameter.Gen

  type Context = org.scalameter.Context
  val Context = org.scalameter.Context

  type Bench[T] = org.scalameter.Bench[T]
  val Bench = org.scalameter.Bench

  @deprecated("Please use Bench instead", "0.7")
  type PerformanceTest[T] = org.scalameter.Bench[T]
  @deprecated("Please use Bench instead", "0.7")
  val PerformanceTest = org.scalameter.Bench

  type Executor[T] = org.scalameter.Executor[T]
  val Executor = org.scalameter.Executor

  type Reporter[T] = org.scalameter.Reporter[T]
  val Reporter = org.scalameter.Reporter

  type Persistor = org.scalameter.Persistor
  val Persistor = org.scalameter.Persistor

  /* execution */

  val LocalExecutor = execution.LocalExecutor
  val SeparateJvmsExecutor = execution.SeparateJvmsExecutor

  val Aggregator = org.scalameter.Aggregator
  val Warmer = org.scalameter.Warmer
  val Measurer = org.scalameter.Measurer

  type Aggregator[T] = org.scalameter.Aggregator[T]
  type Warmer = org.scalameter.Warmer
  type Measurer[T] = org.scalameter.Measurer[T]

  /* reporting */

  type HtmlReporter[T] = reporting.HtmlReporter[T]
  val HtmlReporter = reporting.HtmlReporter

  type LoggingReporter[T] = reporting.LoggingReporter[T]
  val LoggingReporter = reporting.LoggingReporter

  type ValidationReporter[T] = reporting.ValidationReporter[T]
  val ValidationReporter = reporting.ValidationReporter

  type RegressionReporter[T] = reporting.RegressionReporter[T]
  val RegressionReporter = reporting.RegressionReporter

  type DsvReporter[T] = reporting.DsvReporter[T]
  val DsvReporter = reporting.DsvReporter

  type PGFPlotsReporter[T >: Null] = reporting.PGFPlotsReporter[T]
  val PGFPlotsReporter = reporting.PGFPlotsReporter

  val Tester = reporting.RegressionReporter.Tester
  val Historian = reporting.RegressionReporter.Historian

  /* persistence */

  type SerializationPersistor = persistence.SerializationPersistor
  val SerializationPersistor = persistence.SerializationPersistor

  type JSONSerializationPersistor = persistence.JSONSerializationPersistor
  val JSONSerializationPersistor = persistence.JSONSerializationPersistor

  type GZIPJSONSerializationPersistor = persistence.GZIPJSONSerializationPersistor
  val GZIPJSONSerializationPersistor = persistence.GZIPJSONSerializationPersistor

  /* annotation based frontend */

  type benchmark = japi.annotation.benchmark
  type ctx = japi.annotation.ctx
  type curve = japi.annotation.curve
  type gen = japi.annotation.gen
  type disabled = japi.annotation.disabled
  type scopeCtx = japi.annotation.scopeCtx
  type scopes = japi.annotation.scopes
  type setup = japi.annotation.setup
  type setupBeforeAll = japi.annotation.setupBeforeAll
  type teardown = japi.annotation.teardown
  type teardownAfterAll = japi.annotation.teardownAfterAll
  type warmup = japi.annotation.warmup
}
