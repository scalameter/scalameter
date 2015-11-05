package org.scalameter

case class CurveData[T](measurements: Seq[Measurement[T]],
  info: Map[Key[_], Any], context: Context) {
  def success = measurements.forall(_.success)
}

object CurveData {
  def empty[T] = CurveData[T](Seq(), Map(), currentContext)
}
