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
    implicit def listPickler[T: Pickler]: Pickler[List[T]] = new ListPickler[T]
    implicit def vectorPickler[T: Pickler]: Pickler[Vector[T]] = new VectorPickler[T]
    implicit def optionPickler[T: Pickler]: Pickler[Option[T]] = new OptionPickler[T]
    implicit def seqPickler[T: Pickler]: Pickler[scala.collection.Seq[T]] = new SeqPickler[T]

    // tuples
    implicit def tuplePickler[A: Pickler, B: Pickler]: Pickler[(A, B)] =
      new TuplePickler[A, B](implicitly[Pickler[A]], implicitly[Pickler[B]])

    // functions
    implicit def function1[T, S] = new Function1Pickler[T, S]

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
