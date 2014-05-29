package org.scalameter
package examples



import reporting._
import Key._



class TestSuite extends PerformanceTest.OfflineRegressionReport {
  include[MemoryTest]
  include[RegressionTest]
}















