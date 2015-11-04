package org.scalameter
package utils


/** Interface defining standard statistics distributions 
 *  needed for [[org.scalameter.utils.Statistics]]
 */
trait AbstractDistributions {
  /**
   * Quantile function for the Student's t distribution.
   *  Let 0 < p < 1. The p-th quantile of the cumulative distribution function F(x) is defined as
   *  x_p = inf{x : F(x) >= p}
   *  For most of the continuous random variables, x_p is unique and is equal to x_p = F^(-1)(p), where
   *  F^(-1) is the inverse function of F. Thus, x_p is the value for which Pr(X <= x_p) = p. In particular,
   *  the 0.5-th quantile is called the median of F.
   */
  def qt(p: Double, df: Double): Double

  /**
   * Quantile function for the standard (μ = 0, σ = 1) normal distribution.
   */
  def qsnorm(p: Double): Double

  /**
   * Quantile function for the F distribution.
   */
  def qf(p: Double, df1: Double, df2: Double): Double
}