package org.scalameter
package utils

import scala.scalajs.js

/** Implementation of statistics distributions for scalajs */
trait Distributions extends AbstractDistributions {

  def qt(p: Double, df: Double): Double = {
    js.Dynamic.global.jStat.studentt.inv(p, df).asInstanceOf[Double]
  }

  def qsnorm(p: Double): Double = {
    js.Dynamic.global.jStat.normal.inv(p, 0, 1).asInstanceOf[Double]
  }

  def qf(p: Double, df1: Double, df2: Double): Double = {
    js.Dynamic.global.jStat.centralF.inv(p, df1, df2).asInstanceOf[Double]
  }
}


