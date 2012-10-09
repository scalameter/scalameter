package org.collperf



import collection._
import compat.Platform



trait Executor {

  def run[T](benchmark: Setup[T]): CurveData

}




