package org.scalameter
package collections






class CollectionBenchmarks extends PerformanceTest.Regression {

  def persistor = new persistance.SerializationPersistor

  include[TraversableBenchmarks]
  include[SeqBenchmarks]
  include[MapBenchmarks]
  include[SetBenchmarks]

}