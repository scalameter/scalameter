package org.scalameter

import org.scalameter.picklers.Pickler
import scala.collection.Seq

@SerialVersionUID(-2541697615491239986L)
case class Measurement[T: Pickler](value: T, params: Parameters,
  data: MeasurementData[T], units: String) {
  private[scalameter] def pickler: Pickler[T] = implicitly[Pickler[T]]

  def complete: Seq[T] = data.complete
  def success: Boolean = data.success
  def failed: Measurement[T] = this.copy(data = data.copy(success = false))
}

object Measurement {
  implicit def ordering[T] = Ordering.by[Measurement[T], Parameters](_.params)
}
