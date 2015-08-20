package org.scalameter



import org.scalameter.utils.Tree



trait Reporter[T] extends Serializable {
  def report(result: CurveData[T], persistor: Persistor): Unit
  def report(results: Tree[CurveData[T]], persistor: Persistor): Boolean
}


object Reporter {
  def None[T] = new Reporter[T] {
    def report(result: CurveData[T], persistor: Persistor) {}
    def report(results: Tree[CurveData[T]], persistor: Persistor) = true
  }

  case class Composite[T](rs: Reporter[T]*) extends Reporter[T] {
    def report(result: CurveData[T], persistor: Persistor) = for (r <- rs) r.report(result, persistor)
    def report(results: Tree[CurveData[T]], persistor: Persistor) = {
      val oks = for (r <- rs) yield r.report(results, persistor)
      oks.forall(_ == true)
    }
  }
}











