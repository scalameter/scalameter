package org.scalameter
package reporting



import utils.Tree



case class LoggingReporter() extends Reporter {

  def report(result: CurveData, persistor: Persistor) {
    // output context
    log(s"::Benchmark ${result.context.scope}::")
    for ((key, value) <- result.context.properties.filterKeys(Context.machine.properties.keySet.contains).toSeq.sortBy(_._1)) {
      log(s"$key: $value")
    }

    // output measurements
    for (measurement <- result.measurements) {
      log(s"${measurement.params}: ${measurement.time} ms")
    }

    // add a new line
    log("")
  }

  def report(result: Tree[CurveData], persistor: Persistor) = true

}


object LoggingReporter





