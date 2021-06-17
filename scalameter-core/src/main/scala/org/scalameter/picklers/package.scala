package org.scalameter






package object picklers {
}


package picklers {

  import java.util.Date

  import org.scalameter.utils.ClassPath

  object Implicits {
    // primitives
    implicit val unitPickler: PrimitivePickler[Unit] = UnitPickler
    implicit val bytePickler: PrimitivePickler[Byte] = BytePickler
    implicit val booleanPickler: PrimitivePickler[Boolean] = BooleanPickler
    implicit val charPickler: PrimitivePickler[Char] = CharPickler
    implicit val shortPickler: PrimitivePickler[Short] = ShortPickler
    implicit val intPickler: PrimitivePickler[Int] = IntPickler
    implicit val longPickler: PrimitivePickler[Long] = LongPickler
    implicit val floatPickler: PrimitivePickler[Float] = FloatPickler
    implicit val doublePickler: PrimitivePickler[Double] = DoublePickler

    // additional simple types
    implicit val stringPickler: Pickler[String] = StringPickler
    implicit val datePickler: Pickler[Date] = DatePickler
    implicit def enumPickler[T <: Enum[T]]: Pickler[T] =
      EnumPickler.asInstanceOf[Pickler[T]]

    // containers
    implicit val stringListPickler: Pickler[List[String]] = StringListPickler
    implicit val longSeqPickler: Pickler[scala.collection.Seq[Long]] = LongSeqPickler
    implicit val dateOptionPickler: Pickler[Option[Date]] = DateOptionPickler

    // functions
    implicit def function1[T, S]: Pickler[T => S] = new Function1Pickler[T, S]

    /* More complex picklers */

    // pickler for serializing ClassPath
    implicit val classPathPickler: Pickler[ClassPath] = ClassPathPickler
  }

  object noPickler {
    implicit def instance[T]: Pickler[T] = errorPickler.asInstanceOf[Pickler[T]]

    private val errorPickler = new Pickler[Any] {
      private def throwError = sys.error(
        "This Pickler is not intended to use with json based persistors. " +
          "Please use SerializationPersistor instead."
      )

      def pickle(x: Any): Array[Byte] = throwError

      def unpickle(a: Array[Byte], from: Int): (Any, Int) = throwError
    }
  }
}
