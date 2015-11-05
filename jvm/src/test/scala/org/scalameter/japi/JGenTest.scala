package org.scalameter.japi

import org.scalameter.Gen
import org.scalameter.picklers.Implicits._
import org.scalatest.{FunSuite, Matchers}


class JGenTest extends FunSuite with Matchers {
  private def validateJGen[T](jgen: JGen[T], gen:Gen[T]): Unit = {
    jgen.asScala().dataset.toList should
      contain theSameElementsInOrderAs gen.dataset.toList
    jgen.asScala().dataset.zip(gen.dataset).foreach { case (j, s) =>
      jgen.asScala().generate(j) should === (gen.generate(s))
    }
  }

  test("JGen should correctly define simple parameters") {
    validateJGen(
      JGen.exponential("x", 500, 5000, 2),
      Gen.exponential("x")(500, 5000, 2).asInstanceOf[Gen[java.lang.Integer]]
    )
  }

  test("JGen should allow correctly define map combinator") {
    validateJGen(
      JGen.longValues("x", 100l, 1000l, 10000l).map(
        new Fun1[java.lang.Long, List[Long]] {
          def apply(v: java.lang.Long): List[Long] =
            List.fill(100)(v)
        }
      ),
      Gen.enumeration("x")(100l, 1000l, 10000l).map(List.fill(100)(_))
    )
  }

  test("JGen should correctly produce cross product") {
    validateJGen(
      JGen.range("x", 100, 1000, 100).zip(JGen.booleanValues("y", true, false)),
      Gen.range("x")(100, 1000, 100).zip(Gen.enumeration("y")(true, false))
        .asInstanceOf[Gen[(java.lang.Integer, java.lang.Boolean)]]
    )

    validateJGen(
      JGen.crossProduct(
        JGen.intValues("x", 1, 2, 3),
        JGen.booleanValues("y", true, false),
        JGen.charValues("z", 'a', 'b', 'c', 'd', 'e')
      ),
      Gen.crossProduct(
        Gen.enumeration("x")(1, 2, 3),
        Gen.enumeration("y")(true, false),
        Gen.enumeration("z")('a', 'b', 'c', 'd', 'e')
      ).asInstanceOf[Gen[(java.lang.Integer, java.lang.Boolean, java.lang.Character)]]
    )
  }
}
