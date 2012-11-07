package org.scalameter
package execution



import collection._
import utils.Tree



/** Runs a separate JVM instance per setup (i.e. per one curve).
 *
 *  Each separate JVM instance then uses the `LocalExecutor` to run the test.
 */
class JvmPerSetupExecutor(val aggregator: Aggregator, val measurer: Executor.Measurer) extends Executor {

  val runner = new JvmRunner

  def runSetup[T](setup: Setup[T]): CurveData = {
    val initial = initialContext
    val agg = aggregator
    val m = measurer
    val jvmContext = createJvmContext(setup.context)
    runner.run(jvmContext) {
      dyn.initialContext.value = initial
      val delegate = new LocalExecutor(agg, m)
      delegate.runSetup(setup)
    }
  }

  override def toString = s"JvmPerSetupExecutor(${aggregator.name}, ${measurer.name})"

}


object JvmPerSetupExecutor extends Executor.Factory[JvmPerSetupExecutor] {

  def apply(agg: Aggregator, m: Executor.Measurer) = new JvmPerSetupExecutor(agg, m)

}

















