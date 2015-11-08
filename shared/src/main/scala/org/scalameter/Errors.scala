package org.scalameter

import scala.Numeric.Implicits._

class Errors[T: Numeric](measurement: Measurement[T]) {
  import measurement._

  /** [[http://en.wikipedia.org/wiki/Average Average]] of the set of measurements */
  lazy val average = complete.sum.toDouble() / complete.length
  /** [[http://en.wikipedia.org/wiki/Standard_deviation Standard deviation]] of the set of measurements*/
  lazy val sdeviation = math.sqrt(variance)
  /** [[http://en.wikipedia.org/wiki/Variance Variance]] of the set of measurements */
  lazy val variance   = complete.map(_.toDouble() - average)
    .map(x => x * x).sum / (complete.length - 1)
}
