package org.scalameter
package collections




class CollectionBenchmarks extends api.Regression {

  def persistor = new persistence.SerializationPersistor

  include[TraversableBenchmarks]
  include[SeqBenchmarks]
  include[MapBenchmarks]
  include[SetBenchmarks]

}