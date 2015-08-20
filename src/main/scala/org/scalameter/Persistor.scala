package org.scalameter






trait Persistor {
  def load[T](context: Context): History[T]
  def save[T](context: Context, h: History[T]): Unit
}


object Persistor {
  object None extends Persistor {
    def load[T](context: Context): History[T] = History(Nil)
    def save[T](context: Context, h: History[T]) {}
  }
}


