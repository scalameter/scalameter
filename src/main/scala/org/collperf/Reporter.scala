package org.collperf



import collection._



trait Reporter extends Serializable {
  def report(result: Seq[Result], persistor: Persistor): Unit
}


object Reporter {
  object None extends Reporter {
    def report(result: Seq[Result], persistor: Persistor) {}
  }

  case class Composite(rs: Reporter*) {
    def report(result: Seq[Result], persistor: Persistor) = for (r <- rs) r.report(result, persistor)
  }
}











