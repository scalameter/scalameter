package org.collperf
package reporting



import utils.Tree



class ConsoleReporter extends Reporter {

  def report(result: CurveData, persistor: Persistor) {
    // output context
    println(s"...:::Benchmark ${result.context.scope}:::...")
    for ((key, value) <- result.context.properties.filterKeys(Context.machine.properties.keySet.contains).toSeq.sortBy(_._1)) {
      println(s"$key: $value")
    }

    // output measurements
    for (measurement <- result.measurements) {
      println(s"${measurement.params}: ${measurement.time}")
    }

    // add a new line
    println()
  }

  def report(result: Tree[CurveData], persistor: Persistor) = for (res <- result) report(res, persistor)

}








