package org.scalameter






trait Persistor {
  def load(context: Context): History
  def save(context: Context, h: History): Unit
}


object Persistor {
  object None extends Persistor {
    def load(context: Context): History = History(Nil)
    def save(context: Context, h: History) {}
  }
}


