package org.collperf



import collection._
import util.parsing.combinator.Parsers



object Main {

  def main(args: Array[String]) {
    // initialize
    // prepare top-level context
    // identify test objects
    // create reporters and persistors
    val configuration = Configuration.fromCommandLineArgs(args)
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

  case class Configuration(benches: Seq[String], reporters: Seq[Reporter], persistor: Persistor)

  object Configuration extends Parsers {
    def fromCommandLineArgs(args: Array[String]) = {
      new Configuration(null, null, null)
    }
  }

}