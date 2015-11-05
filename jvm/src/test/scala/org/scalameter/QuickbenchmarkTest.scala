package org.scalameter



import org.scalatest.FunSuite
import org.scalameter.api._
import util.Properties.javaVersion
import org.scalameter.picklers.Implicits._



class QuickbenchmarkTest extends FunSuite {

  object SomeBenchmark extends Bench.LocalTime {
    performance of "single" in {
      using(Gen.single("single")(1)) in (_ + 0)
    }
  }

  test("A benchmark should start without any command line params for Java 1.7") {
    if (!(javaVersion startsWith ("1.7"))) pending
    SomeBenchmark.main(Array())
  }

  test("A benchmark should start with '-preJDK7' command line flag for Java 1.6") {
    if (!(javaVersion startsWith ("1.6"))) pending
    SomeBenchmark.main(Array("-preJDK7"))
  }

}