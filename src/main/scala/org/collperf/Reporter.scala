package org.collperf



import collection._



trait Reporter extends Serializable {
  def report(result: Result, persistor: Persistor): Unit
  def flush(): Unit
}


object Reporter {
  object None extends Reporter {
    def report(result: Result, persistor: Persistor) {}
    def flush() {}
  }

  case class Composite(rs: Reporter*) {
    def report(result: Result, persistor: Persistor) = for (r <- rs) r.report(result, persistor)
    def flush() = for (r <- rs) r.flush()
  }
}











