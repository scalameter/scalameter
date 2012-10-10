package org.collperf






trait Persistor {
  def load(context: Context): History
  def save(result: ResultData): Unit
}


object Persistor {
  object None extends Persistor {
    def load(context: Context): History = History(Nil)
    def save(result: ResultData) {}
  }
}


