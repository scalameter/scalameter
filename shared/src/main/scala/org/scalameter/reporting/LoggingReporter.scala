package org.scalameter
package reporting



import org.scalameter.utils.Tree



case class LoggingReporter[T]() extends Reporter[T] {

  def report(result: CurveData[T], persistor: Persistor) {
    // output context
    log(s"::Benchmark ${result.context.scope}::")
    for ((key, value) <- result.context.properties.filterKeys(Context.machine.properties.keySet.contains).toSeq.sortBy(_._1)) {
      log(s"$key: $value")
    }

    // output measurements
    for (measurement <- result.measurements) {
      log(s"${measurement.params}: ${measurement.value}")
    }

    // add a new line
    log("")
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor) = true

}


object LoggingReporter





