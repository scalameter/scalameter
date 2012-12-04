package org.scalameter
package reporting



import utils.Tree



case class DsvReporter(delimiter: Char) extends Reporter {

  def report(result: CurveData, persistor: Persistor) {
  }

  def report(result: Tree[CurveData], persistor: Persistor) = {
    // TODO

    true
  }

}


object DsvReporter


