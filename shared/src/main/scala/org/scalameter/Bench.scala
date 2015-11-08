package org.scalameter



import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.Pickler



/** Base class for ScalaMeter benchmarks.
 */
abstract class Bench[U] extends DSL[U] with Serializable {

  def main(args: Array[String]) {
    val ctx = dyn.currentContext.value ++
      Main.Configuration.fromCommandLineArgs(args).context
    val ok = withTestContext(ctx, Log.Console, Events.None) {
      executeTests()
    }

    if (!ok) sys.exit(1)
  }

}

object Bench {

  class Group extends Bench[Nothing] with GroupedPerformanceTest {
    def measurer = Measurer.None
    def executor = Executor.None
    def persistor = Persistor.None
    def reporter = Reporter.None
  }

}
