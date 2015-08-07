package org.scalameter
package examples



import reporting._
import Key._



class TestSuite extends Bench.OfflineRegressionReport {
  include[MemoryTest]
  include[RegressionTest]
}















