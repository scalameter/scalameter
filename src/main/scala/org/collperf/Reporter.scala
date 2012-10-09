package org.collperf



import collection._



trait Reporter extends Serializable {
  def report(result: ResultData, persistor: Persistor): Unit
}


object Reporter {
  object None extends Reporter {
    def report(result: ResultData, persistor: Persistor) {}
  }

  case class Composite(rs: Reporter*) {
    def report(result: ResultData, persistor: Persistor) = for (r <- rs) r.report(result, persistor)
  }
}











