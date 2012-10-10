package org.collperf






trait Persistor {
  def load(context: Context): History
  def save(context: Context, result: Seq[CurveData]): Unit
}


object Persistor {
  object None extends Persistor {
    def load(context: Context): History = History(Nil)
    def save(context: Context, result: Seq[CurveData]) {}
  }
}


