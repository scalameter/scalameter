package org.scalameter
package utils


import language.implicitConversions

import collection._
import math._
import org.apache.commons.math3.distribution.TDistribution
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.FDistribution



/** Standard statistics utilities.
 *
 *  Note: significance level `alpha` is equal to `1 - confidenceLevel`. If you want to
 *  be sure that 2 sets of measurements do not differ with `90` percent probability, then
 *  the significance level `alpha` should be set to `0.1`.
 *  In this example, the confidence level is `0.9`, and the significance level is `0.1`.
 */
object Statistics {

	trait Test {
		def passed: Boolean
	}

	implicit def test2boolean(t: Test) = t.passed

	/** Let Y = (Y_1, ..., Y_n) data resulting from a parametric law F of
	 *  scalar parameter θ. A confidence interval (B_i, B_s) is a statistic
	 *  in the form of an interval containing θ with a specified probability.
	 */
	def confidenceInterval(seq: Seq[Double], alpha: Double): (Double, Double) = {
		val n = seq.length
		val xbar = mean(seq)
		val S = stdev(seq)
		/* Student's distribution could be used all the time because it converges
		 * towards the normal distribution as n grows.
		 */
		if (n < 30) {
			(xbar - qt(1 - alpha / 2, n - 1) * S / sqrt(n), xbar + qt(1 - alpha / 2, n - 1) * S / sqrt(n))
		} else {
			(xbar - qsnorm(1 - alpha / 2) * S / sqrt(n), xbar + qsnorm(1 - alpha / 2) * S / sqrt(n))
		}
	}

	abstract class Standard2WayTest extends Test {
		def alpha: Double
		def alt1: Seq[Double]
		def alt2: Seq[Double]

		val m1 = mean(alt1)
		val m2 = mean(alt2)
		val s1 = stdev(alt1)
		val s2 = stdev(alt2)
		val n1 = alt1.length
		val n2 = alt2.length

		def ci1 = confidenceInterval(alt1, alpha)
		def ci2 = confidenceInterval(alt2, alpha)
	}

	/** Compares two alternative sets of measurements given a significance level `alpha`.
	 *  
	 *  @param strict   if `true`, the confidence interval test is strict - the confidence interval overlap
	 *                  of the alternatives will not be additionally compared
	 *  @return         returns `true` if there is no statistical difference for s.l. `alpha`
	 */
	case class ConfidenceIntervalTest(strict: Boolean, alt1: Seq[Double], alt2: Seq[Double], alpha: Double) extends Standard2WayTest {
		val diffM = m1 - m2
		val diffS = sqrt(s1 * s1 / n1 + s2 * s2 / n2)
		val ndf = math.round(pow(pow(s1, 2) / n1 + pow(s2, 2) / n2, 2) / (pow(pow(s1, 2) / n1, 2) / (n1 - 1) + pow(pow(s2, 2) / n2, 2) / (n2 - 1)))
		val ci = if ((ndf != 0) && (n1 < 30 || n2 < 30)) {
			(diffM - qt(1 - alpha / 2, ndf) * diffS, diffM + qt(1 - alpha / 2, ndf) * diffS)
		} else {
			(diffM - qsnorm(1 - alpha / 2) * diffS, diffM + qsnorm(1 - alpha / 2) * diffS)
		}

		val overlapping = OverlapTest(alt1, alt2, alpha, 0.0).passed

		val strictPassed = ci._1 <= 0 && 0 <= ci._2

		val relaxedPassed = strictPassed || overlapping

    /** If 0 is within the confidence interval of the mean difference, or confidence intervals overlap,
     *  we conclude that there is no statistical difference between the two alternatives.
     */
		val passed = if (strict) strictPassed else relaxedPassed
	}

	/** Computes the confidence interval of the two alternatives.
	 *  Passes if the confidence intervals overlap at the given significance level `alpha`.
	 *
	 *  Applies relative noise before doing the actual overlap test - the relative noise may
	 *  increase the confidence interval further, but it will not shrink it.
	 */
	case class OverlapTest(alt1: Seq[Double], alt2: Seq[Double], alpha: Double, noiseMagnitude: Double) extends Standard2WayTest {
		def noised(ci: (Double, Double)) = {
			val mean = (ci._1 + ci._2) / 2
			val n = noiseMagnitude * mean
			(math.min(ci._1, mean - n), math.max(ci._2, mean + n))
		}

		override def ci1 = noised(super.ci1)
		override def ci2 = noised(super.ci2)

		private def in(x: Double, int: (Double, Double)) = x >= int._1 && x <= int._2

		val passed = in(ci1._1, ci2) || in(ci1._2, ci2) || in(ci2._1, ci1) || in(ci2._2, ci1)
	}

	/** Computes sum-of-squares due to differences between alternatives. */
	def SSA(alternatives: Seq[Seq[Double]]): Double = {
		val means: Seq[Double] = for(a <- alternatives) yield mean(a)
		val overallMean: Double = means.reduceLeft(_ + _) / means.length

		(means zip alternatives.map(_.length)).foldLeft(0.0) { (sum: Double, p: (Double, Int)) =>
			val yi = p._1
			val ni = p._2
			sum + ni * (yi - overallMean) * (yi - overallMean)
	  }
	}

	/** Computes sum-of-squares due to errors in measurements. */
	def SSE(alternatives: Seq[Seq[Double]]): Double = {
		val means: Seq[Double] = for(a <- alternatives) yield mean(a)
		val doubleSumTerms = for ((alternative, mean) <- alternatives zip means; yij <- alternative) yield (yij - mean) * (yij - mean)
		doubleSumTerms reduceLeft (_ + _)
	}

	/** ANOVA separates the total variation in a set of measurements into a component due to random fluctuations
	 *  in the measurements and a component due to the actual differences among the alternatives.
	 *  
	 *  If the variation between the alternatives is larger than the variation within each alternative, then
	 *  it can be concluded that there is a statistically significant difference between the alternatives.
	 *  
	 *  For more information see:
	 *  Andy Georges, Dries Buytaert, Lieven Eeckhout - Statistically Rigorous Java Performance Evaluation
	 */
	case class ANOVAFTest(alternatives: Seq[Seq[Double]], alpha: Double) extends Test {
		/* Computation of SSA */
		val ssa = SSA(alternatives)

		/* Computation of SSE */
		val sse = SSE(alternatives)

		val K = alternatives.length
		val N = alternatives.foldLeft(0)(_ + _.size)
		val F = ssa / sse * (N - K) / (K - 1)
		val quantile = qf(1 - alpha, K - 1, N - K)

		def passed = F <= quantile || sse == 0.0
	}

	def CoV(measurements: Seq[Double]) = stdev(measurements) / mean(measurements)

	/** Compares the coefficient of variance to some `threshold` value.
	 *  
	 *  This heuristic can be used to detect if the measurement has stabilized.
	 */
	case class CoVTest(measurements: Seq[Double], threshold: Double) extends Test {
		val cov = CoV(measurements)

		def passed = cov <= threshold
	}

	/** Computes the mean of the sequence of measurements. */
	def mean(seq : Seq[Double]): Double = seq.sum * 1.0 / seq.length

	/** The sample standard sample deviation. It is the square root of S², unbiased estimator for the variance.
	 */
	def stdev(seq: Seq[Double]): Double = {
		val xbar = mean(seq)
		val squaresum: Double = seq.foldLeft(0.0)((sum, xi) => sum + (xi - xbar) * (xi - xbar))
		sqrt(squaresum / (seq.length - 1))
	}

	def clamp(x: Double, below: Double, above: Double) = math.max(below, math.min(above, x))

	/** Quantile function for the Student's t distribution.
	 *  Let 0 < p < 1. The p-th quantile of the cumulative distribution function F(x) is defined as
	 *  x_p = inf{x : F(x) >= p}
	 *  For most of the continuous random variables, x_p is unique and is equal to x_p = F^(-1)(p), where
	 *  F^(-1) is the inverse function of F. Thus, x_p is the value for which Pr(X <= x_p) = p. In particular,
	 *  the 0.5-th quantile is called the median of F.
	 */
	private def qt(p: Double, df: Double): Double = {
		new TDistribution(df).inverseCumulativeProbability(p)
	}

	/** Quantile function for the standard (μ = 0, σ = 1) normal distribution.
	 */
	private def qsnorm(p: Double): Double = {
		new NormalDistribution().inverseCumulativeProbability(p)
	}

	/** Quantile function for the F distribution.
	 */
	private def qf(p: Double, df1: Double, df2: Double) = {
		new FDistribution(df1, df2).inverseCumulativeProbability(p)
	}

}













