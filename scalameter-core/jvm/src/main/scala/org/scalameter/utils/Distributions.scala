package org.scalameter
package utils

import org.apache.commons.math3.distribution.TDistribution
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.FDistribution


/** Implementation of statistics distributions for the jvm */
trait Distributions extends AbstractDistributions {
  
  def qt(p: Double, df: Double): Double = {
    new TDistribution(df).inverseCumulativeProbability(p)
  }

  def qsnorm(p: Double): Double = {
    new NormalDistribution().inverseCumulativeProbability(p)
  }

  def qf(p: Double, df1: Double, df2: Double): Double = {
    new FDistribution(df1, df2).inverseCumulativeProbability(p)
  }
}

