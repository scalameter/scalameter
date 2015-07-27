package org.scalameter


package object picklers {

}


package picklers {
  object Implicits {
    // primitives
    implicit val unitPickler = UnitPickler
    implicit val bytePickler = BytePickler
    implicit val booleanPickler = BooleanPickler
    implicit val charPickler = CharPickler
    implicit val shortPickler = ShortPickler
    implicit val intPickler = IntPickler
    implicit val longPickler = LongPickler
    implicit val floatPickler = FloatPickler
    implicit val doublePickler = DoublePickler

    // additional simple types
    implicit val stringPickler = StringPickler
    implicit val datePickler = DatePickler
    implicit def enumPickler[T <: Enum[T]]: Pickler[T] =
      EnumPickler.asInstanceOf[Pickler[T]]

    // containers
    implicit val stringListPickler = StringListPickler
    implicit val longSeqPickler = LongSeqPickler
    implicit val dateOptionPickler = DateOptionPickler

    /* More complex picklers */

    // pickler for serializing ClassPath
    implicit val classPathPickler = ClassPathPickler
  }

  object noPickler {
    implicit def instance[T] = errorPickler.asInstanceOf[Pickler[T]]

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
