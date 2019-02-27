package org.scalameter
package reporting



import org.scalameter.utils.Tree



/** Simply logs the measurement data to the standard output.
 */
case class LoggingReporter[T]() extends Reporter[T] {

  def report(result: CurveData[T], persistor: Persistor): Unit = {
    // output context
    log.report(s"::Benchmark ${result.context.scope}::")
    val machineKeys = result.context.properties
      .filterKeys(Context.machine.properties.keySet.contains).toSeq.sortBy(_._1)
    for ((key, value) <- machineKeys) {
      log.report(s"$key: $value")
    }

    // output measurements
    for (measurement <- result.measurements) {
      log.report(s"${measurement.params}: ${measurement.value} ${measurement.units}")
    }

    // add a new line
    log.report("")
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor) = true

}
