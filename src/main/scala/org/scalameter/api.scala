package org.scalameter

/** Import the contents of this singleton object to obtain access to most abstractions
 *  in the ScalaMeter API.
 *
 *  Note that some definitions might shadow others, so if you import the contents of this
 *  object, you should not import the contents of other packages directly.
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

  type PerformanceTest = org.scalameter.PerformanceTest
  val PerformanceTest = org.scalameter.PerformanceTest

  type Executor = org.scalameter.Executor
  val Executor = org.scalameter.Executor

  type Reporter = org.scalameter.Reporter
  val Reporter = org.scalameter.Reporter

  type Persistor = org.scalameter.Persistor
  val Persistor = org.scalameter.Persistor

  /* execution */

  val LocalExecutor = execution.LocalExecutor
  val SeparateJvmsExecutor = execution.SeparateJvmsExecutor

  val Aggregator = org.scalameter.Aggregator
  val Warmer = org.scalameter.Warmer
  val Measurer = org.scalameter.Measurer

  type Warmer = org.scalameter.Warmer
  type Measurer = org.scalameter.Measurer

  /* reporting */

  type ChartReporter = reporting.ChartReporter
  val ChartReporter = reporting.ChartReporter

  type HtmlReporter = reporting.HtmlReporter
  val HtmlReporter = reporting.HtmlReporter

  type LoggingReporter = reporting.LoggingReporter
  val LoggingReporter = reporting.LoggingReporter

  type RegressionReporter = reporting.RegressionReporter
  val RegressionReporter = reporting.RegressionReporter

  type DsvReporter = reporting.DsvReporter
  val DsvReporter = reporting.DsvReporter

  val Tester = reporting.RegressionReporter.Tester
  val Historian = reporting.RegressionReporter.Historian
  val ChartFactory = reporting.ChartReporter.ChartFactory

  /* persistence */

  type SerializationPersistor = persistence.SerializationPersistor
  val SerializationPersistor = persistence.SerializationPersistor

  type JSONSerializationPersistor = persistence.JSONSerializationPersistor
  val JSONSerializationPersistor = persistence.JSONSerializationPersistor

  type GZIPJSONSerializationPersistor = persistence.GZIPJSONSerializationPersistor
  val GZIPJSONSerializationPersistor = persistence.GZIPJSONSerializationPersistor
}
