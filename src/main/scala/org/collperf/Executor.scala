package org.collperf



import collection._
import utils.Tree



trait Executor {

  def run[T](setups: Tree[Setup[T]]): Tree[CurveData]

}




