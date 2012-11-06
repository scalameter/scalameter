package org.scalameter



import collection._
import utils.Tree



trait Reporter extends Serializable {
  def report(result: Tree[CurveData], persistor: Persistor): Unit
}


object Reporter {
  object None extends Reporter {
    def report(result: Tree[CurveData], persistor: Persistor) {}
  }

  case class Composite(rs: Reporter*) extends Reporter {
    def report(result: Tree[CurveData], persistor: Persistor) = for (r <- rs) r.report(result, persistor)
  }
}











