package org.scalameter

object Aggregator {

  def apply(n: String)(f: Seq[Double] => Double) = new Aggregator {
    def name = n
    def apply(times: Seq[Double]) = f(times)
    def data(times: Seq[Double]) = MeasurementData(times, true)
  }

  def min = Aggregator("min") { _.min }

  def max = Aggregator("max") { _.max }

  def median = Aggregator("median") {
    xs =>
    val sorted = xs.sorted
    sorted(sorted.size / 2)
  }

  def average = Aggregator("average") { utils.Statistics.mean }

  def stdev = Aggregator("stdev") { utils.Statistics.stdev }

  @deprecated("Unnecessary, use a directly", "0.5")
  def complete(a: Aggregator) = a
}

trait Aggregator extends (Seq[Double] => Double) with Serializable {
  def name: String
  def apply(times: Seq[Double]): Double
  def data(times: Seq[Double]): MeasurementData
}