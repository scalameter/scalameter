package org.collperf
package execution



import collection._
import utils.Tree



class JvmPerSetupExecutor(val aggregator: Aggregator, val measurer: Executor.Measurer) extends Executor {

  val runner = new JvmRunner

  def maxHeap = 2048

  def startHeap = 2048

  def run[T](setuptree: Tree[Setup[T]]): Tree[CurveData] = {
    for (setup <- setuptree) yield runSetup(setup)
  }

  private[execution] def runSetup[T](setup: Setup[T]): CurveData = {
    val initial = initialContext
    val agg = aggregator
    val m = measurer
    runner.run(jvmflags()) {
      initialContext = initial
      val delegate = new LocalExecutor(agg, m)
      delegate.runSetup(setup)
    }
  }

  override def toString = s"JvmPerSetupExecutor(${aggregator.name}, ${measurer.name})"

}


object JvmPerSetupExecutor extends Executor.Factory[JvmPerSetupExecutor] {

  def apply(agg: Aggregator, m: Executor.Measurer) = new JvmPerSetupExecutor(agg, m)

}

















