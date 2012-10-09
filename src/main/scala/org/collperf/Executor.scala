package org.collperf



import collection._
import compat.Platform



trait Executor {

  def run[T](setups: Seq[Setup[T]]): Seq[CurveData]

}




