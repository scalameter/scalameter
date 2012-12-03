package org.scalameter
package collections.fast





class CollectionBenchmarks extends PerformanceTest.Regression {

  def persistor = new persistence.SerializationPersistor

  include[TraversableBenchmarks]
  include[SeqBenchmarks]
  include[MapBenchmarks]
  include[SetBenchmarks]

}