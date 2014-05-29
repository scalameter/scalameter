package org.scalameter


import collection._
import compat._
import utils.{withGCNotification, Tree}



trait Executor {

  def run[T](setuptree: Tree[Setup[T]], reporter: Reporter, persistor: Persistor): Tree[CurveData] = {
    for (setup <- setuptree) yield {
      val exec = Option(setup.customExecutor).getOrElse(this)
      val cd = exec.runSetup(setup)
      reporter.report(cd, persistor)
      cd
    }
  }

  def runSetup[T](setup: Setup[T]): CurveData

}


object Executor {

  import Key._

  object None extends Executor {
    def runSetup[T](setup: Setup[T]): CurveData = ???
  }

  trait Factory[E <: Executor] {
    def apply(warmer: Warmer, aggregator: Aggregator, m: Measurer): E
  }
  
}






















