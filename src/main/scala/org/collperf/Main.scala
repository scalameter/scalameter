package org.collperf



import collection._



object Main {

  def main(args: Array[String]) {
    // initialize
    // prepare top-level context
    // identify test objects
    // create reporters and persistors
    val configuration = new Configuration(args)
    import configuration._

    // schedule benchmarks
    for (bench <- benches) {
      // TODO
    }

    // execute all benchmark objects
    for (benchmark <- Runner.flushSchedule()) {
      // execute tests
      val result: Result = Runner.run(benchmark)

      // generate reports
      for (r <- reporters) r.report(result, persistor)
    }
  }

  class Configuration(args: Array[String]) {
    val benches: Seq[String] = {
      null
    }

    val reporters: Seq[Reporter] = {
      null
    }

    val persistor: Persistor = {
      null
    }
  }

}