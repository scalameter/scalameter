package org.scalameter



import collection._
import utils.Tree



trait Reporter extends Serializable {
  def report(result: CurveData, persistor: Persistor): Unit
  def report(results: Tree[CurveData], persistor: Persistor): Boolean
}


object Reporter {
  object None extends Reporter {
    def report(result: CurveData, persistor: Persistor) {}
    def report(results: Tree[CurveData], persistor: Persistor) = true
  }

  case class Composite(rs: Reporter*) extends Reporter {
    def report(result: CurveData, persistor: Persistor) = for (r <- rs) r.report(result, persistor)
    def report(results: Tree[CurveData], persistor: Persistor) = {
      val oks = for (r <- rs) yield r.report(results, persistor)
      oks.forall(_ == true)
    }
  }
}











