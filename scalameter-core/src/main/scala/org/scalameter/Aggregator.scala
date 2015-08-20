package org.scalameter

object Aggregator {

  object Implicits {
    implicit def mapOrdering[T]: Ordering[Map[String, T]] = Ordering.by(_.keys)
  }

  def apply[T](n: String)(f: Seq[Quantity[T]] => Quantity[T]) = new Aggregator[T] {
    def name = n

    def apply(times: Seq[Quantity[T]]) = f(times)

    def data(times: Seq[Quantity[T]]) = MeasurementData(times.map(_.value), true)
  }

  def min[T: Ordering]: Aggregator[T] = Aggregator("min") { _.minBy(_.value) }

  def max[T: Ordering]: Aggregator[T] = Aggregator("max") { _.maxBy(_.value) }

  def median[T: Ordering]: Aggregator[T] = Aggregator("median") { xs =>
    val sorted = xs.sortBy(_.value)
    sorted(sorted.size / 2)
  }

  def average: Aggregator[Double] = Aggregator("average") { xs =>
    val units = xs.head.units
    Quantity(utils.Statistics.mean(xs.map(_.value)), units)
  }

  def stdev: Aggregator[Double] = Aggregator("stdev") { xs =>
    val units = xs.head.units
    Quantity(utils.Statistics.stdev(xs.map(_.value)), units)
  }

  @deprecated("Unnecessary, use a directly", "0.5")
  def complete[T](a: Aggregator[T]) = a
}

trait Aggregator[T] extends (Seq[Quantity[T]] => Quantity[T]) with Serializable { self =>
  def name: String

  def apply(times: Seq[Quantity[T]]): Quantity[T]

  def data(times: Seq[Quantity[T]]): MeasurementData[T]
}
