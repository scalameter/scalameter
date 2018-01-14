package org.scalameter

import java.io.File

import org.junit.Test
import org.junit.Assert.assertTrue

import org.scalameter.api._
import org.scalameter.Bench.OnlineRegressionReport

import JUnitOnlineRegressionReportTest._

class JUnitOnlineRegressionReportTest
extends OnlineRegressionReport  {

  override def defaultConfig = super.defaultConfig ++ Context(
    reports.resultDir -> dir.getAbsolutePath,
    reports.regression.noiseMagnitude -> 0.1
  )

  @Test
  def mytest(): Unit = {

    println(s"Writing report in ${dir.getAbsolutePath}")

    if (!dir.exists) {
      assertTrue("Created reports directory", dir.mkdirs)
    }


    val sizes = Gen.range("size")(1000000, 5000000, 2000000)
    val arrays = for (sz <- sizes) yield (0 until sz).toArray

    performance of "Array" in {
      measure method "foreach" in {
        using(arrays) config (
          exec.independentSamples -> 1
          ) in { xs =>
          var sum = 0
          xs.foreach(x => sum += x)
        }
      }
    }

    assertTrue(s"Ran ${this.getClass.getSimpleName}", this.runBench())
  }

}

object JUnitOnlineRegressionReportTest {

  val dir = new File(
    System.getProperty("java.io.tmpdir") +
    s"/scalaMeterTestResults/${this.getClass.getSimpleName.replace("$", "")}"
  )
}