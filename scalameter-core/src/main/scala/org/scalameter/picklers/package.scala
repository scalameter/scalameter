package org.scalameter


package object picklers {
  object std {
    // primitives
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

    // containers
    implicit val stringListPickler = StringListPickler
    implicit val longSeqPickler = LongSeqPickler
    implicit val dateOptionPickler = DateOptionPickler
  }

  object nopickler {
    implicit def noPickler[T] = errorPickler.asInstanceOf[Pickler[T]]

    private val errorPickler = new Pickler[Any] {
      private def error = sys.error("This Pickler is not intended to use with json based persistors. Please use SerializationPersistor instead.")

      def pickle(x: Any): Array[Byte] = error

      def unpickle(a: Array[Byte]): Any = error
    }
  }
}
