package org.scalameter.picklers






class Function1Pickler[T, S] extends Pickler[T => S] {
  override def pickle(x: T => S): Array[Byte] =
    StringPickler.pickle(x.getClass.getName)

  override def unpickle(a: Array[Byte], from: Int): (T => S, Int) = {
    val (name, pos) = StringPickler.unpickle(a, from)
    (Class.forName(name).asInstanceOf[T => S], pos)
  }
}
