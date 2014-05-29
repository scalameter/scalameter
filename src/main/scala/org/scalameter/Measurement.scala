package org.scalameter

import scala.collection.Seq

@SerialVersionUID(-2541697615491239986L)
case class Measurement(value: Double, params: Parameters, data: MeasurementData, units: String) {
  def complete: Seq[Double] = data.complete
  def success: Boolean = data.success
  def errors: Errors = new Errors(this)
  def failed = this.copy(data = data.copy(success = false))
}

object Measurement {
  implicit val ordering = Ordering.by[Measurement, Parameters](_.params)
}
