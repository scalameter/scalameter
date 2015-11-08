package org.scalameter



import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import org.scalatest.FunSuite
import util.Properties.javaVersion



class GenTest extends FunSuite {

  test("A generator should be renamed") {
    val gen = Gen.range("size")(100, 500, 100)
    assert(gen.dataset.toList  == (100 to 500 by 100).map(x => Parameters(Parameter[Int]("size") -> x)), gen.dataset.toList)

    val renamed = gen.rename("size" -> "thatSize")
    assert(renamed.dataset.toList  == (100 to 500 by 100).map(x => Parameters(Parameter[Int]("thatSize") -> x)), renamed.dataset.toList)

    for ((p, v) <- renamed.dataset.toSeq.zip(100 to 500 by 100)) {
      assert(renamed.generate(p) == v)
    }

    val zipped = gen zip renamed
    val crossProduct = for {
      x <- 100 to 500 by 100
      y <- 100 to 500 by 100
    } yield (x, y)
    for ((p, v) <- zipped.dataset.toSeq.zip(crossProduct)) {
      assert(zipped.generate(p) == v)
    }
  }

}