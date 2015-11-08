package org.scalameter.picklers



import java.io.File
import java.util.Date
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSuite, Matchers}
import org.scalameter.picklers.Implicits._
import org.scalameter.utils.ClassPath



class PicklerSpecification extends FunSuite with PropertyChecks with Matchers {
  def validatePickler[T: Pickler](o: T) = {
    val pickler = implicitly[Pickler[T]]
    val p = pickler.pickle(o)
    val uo = pickler.unpickle(p)
    uo should === (o)
  }

  test("Unit pickling") {
    forAll { o: Unit =>
      validatePickler(o)
    }
  }

  test("Byte pickling") {
    forAll { o: Byte =>
      validatePickler(o)
    }
  }

  test("Boolean pickling") {
    forAll { o: Boolean =>
      validatePickler(o)
    }
  }

  test("Char pickling") {
    forAll { o: Char =>
      validatePickler(o)
    }
  }
  
  test("Short pickling") {
    forAll { o: Short =>
      validatePickler(o)
    }
  }

  test("Int pickling") {
    forAll { o: Int =>
      validatePickler(o)
    }
  }

  test("Long pickling") {
    forAll { o: Long =>
      validatePickler(o)
    }
  }

  test("Float pickling") {
    forAll { o: Float =>
      validatePickler(o)
    }
  }

  test("Double pickling") {
    forAll { o: Double =>
      validatePickler(o)
    }
  }

  test("String pickling") {
    forAll { o: String =>
      validatePickler(o)
    }
  }

  test("Date pickling") {
    forAll { o: Date =>
      validatePickler(o)
    }
  }

  test("List[String] pickling") {
    forAll { o: List[String] =>
      validatePickler(o)
    }
  }

  test("Seq[Long] pickling") {
    forAll { o: Seq[Long] =>
      validatePickler(o)
    }
  }

  test("Option[Date] pickling") {
    forAll { o: Option[Date] =>
      validatePickler(o)
    }
  }

  test("ClassPath pickling") {
    forAll { o: List[String] =>
      validatePickler(ClassPath(
        o.map(s => new File(s.replaceAll(s""""|${File.pathSeparatorChar}""", "")))))
    }
  }

  test("Enum pickling") {
    forAll { o: TestEnum1 =>
      validatePickler(o)
    }

    forAll { o: TestEnum2 =>
      validatePickler(o)
    }
  }
}
