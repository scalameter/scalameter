package org.scalameter

class Errors(measurement: Measurement) {
  import measurement._

  /** [[http://en.wikipedia.org/wiki/Average Average]] of the set of measurements */
  lazy val average = complete.sum / complete.length
  /** [[http://en.wikipedia.org/wiki/Standard_deviation Standard deviation]] of the set of measurements*/
  lazy val sdeviation = math.sqrt(variance)
  /** [[http://en.wikipedia.org/wiki/Variance Variance]] of the set of measurements */
  lazy val variance   = complete.map(_ - average).map(x => x * x).sum / (complete.length - 1)
}
