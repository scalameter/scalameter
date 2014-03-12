package org.scalameter

case class CurveData(measurements: Seq[Measurement], info: Map[Key[_], Any], context: Context) {
  def success = measurements.forall(_.success)
}

object CurveData {
  def empty = CurveData(Seq(), Map(), initialContext)
}
