package org.scalameter
package examples



import reporting._
import Key._



class TestSuite extends PerformanceTest.Regression {
  def persistor = new persistence.SerializationPersistor

  include[MemoryTest]
  include[RegressionTest]
}















