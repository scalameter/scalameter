package org.scalameter
package collections






class CollectionBenchmarks extends Bench.Regression {

  def persistor = new persistence.SerializationPersistor

  include[TraversableBenchmarks]
  include[SeqBenchmarks]
  include[MapBenchmarks]
  include[SetBenchmarks]

}