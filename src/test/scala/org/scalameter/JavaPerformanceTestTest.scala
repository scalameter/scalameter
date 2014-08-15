package org.scalameter



import org.scalatest.FunSuite
import org.scalameter.api._
import util.Properties.javaVersion



class JavaPerformanceTestTest extends FunSuite {

  test("Correctly parse config block") {
    val test = new org.scalameter.examples.JavaRegressionTest3
    test.executeTests()
  }

}