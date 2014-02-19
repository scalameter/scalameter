package org.scalameter

object Aggregator {

  implicit final class SeqDoubleOps(val sq: Seq[Double]) extends AnyVal {
    def mean = sq.sum / sq.size

    def stdev: Double = {
      val m = mean
      var s = 0.0
      for (v <- sq) {
        val diff = v - m
        s += diff * diff
      }
      math.sqrt(s / (sq.size - 1))
    }
  }

  case class Statistic(min: Double, max: Double, average: Double, stdev: Double, median: Double)

  def apply(n: String)(f: Seq[Double] => Double) = new Aggregator {
    def name = n
    def apply(times: Seq[Double]) = f(times)
    def data(times: Seq[Double]) = Measurement.Data(times, true)
  }

  def min = Aggregator("min") { _.min }

  def max = Aggregator("max") { _.max }

  def median = Aggregator("median") {
    xs =>
    val sorted = xs.sorted
    sorted(sorted.size / 2)
  }

  def average = Aggregator("average") { _.mean }

  def stdev = Aggregator("stdev") { _.stdev }

  @deprecated("Unnecessary, use a directly", "0.5")
  def complete(a: Aggregator) = a
}

trait Aggregator extends (Seq[Double] => Double) with Serializable {
  def name: String
  def apply(times: Seq[Double]): Double
  def data(times: Seq[Double]): Measurement.Data
}