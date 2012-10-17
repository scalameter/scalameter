package org.collperf



import collection._
import math._
import org.apache.commons.math3.distribution.TDistribution
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.FDistribution


object Statistics {

	/** Let Y = (Y_1, ..., Y_n) data resulting from a parametric law F of
	 *  scalar parameter θ. A confidence interval (B_i, B_s) is a statistic
	 *  in the form of an interval containing θ with a specified probability.
	 */
	def confidenceInterval(seq: Seq[Long], alpha: Double): (Double, Double) = {
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

	/** Compares two alternative sets of measurements given a confidence level `alpha`.
	 */
	def confidenceIntervalTest(alt1: Seq[Long], alt2: Seq[Long], alpha: Double): Boolean = {
		val m1 = mean(alt1)
		val m2 = mean(alt2)
		val s1 = stdev(alt1)
		val s2 = stdev(alt2)
		val n1 = alt1.length
		val n2 = alt2.length
		confidenceIntervalTest(m1, m2, s1, s2, n1, n2, alpha)
	}

	/** Compares two alternative sets of measurements given a confidence level `alpha`, and
	 *  the mean, deviation and the number of measurements for each set.
	 */
	def confidenceIntervalTest(m1: Double, m2: Double, S1: Double, S2: Double, n1: Int, n2: Int, alpha: Double): Boolean = {
		val diffM = m1 - m2
		val diffS = sqrt(S1 * S1 / n1 + S2 * S2 / n2)
		val CI = if (n1 < 30 || n2 < 30) {
			val ndf = math.round(pow(pow(S1, 2) / n1 + pow(S2, 2) / n2, 2) / (pow(pow(S1, 2) / n1, 2) / (n1 - 1) + pow(pow(S2, 2) / n2, 2) / (n2 - 1)))
			(diffM - qt(1 - alpha / 2, ndf) * diffS, diffM + qt(1 - alpha / 2, ndf) * diffS)
		} else {
			(diffM - qsnorm(1 - alpha / 2) * diffS, diffM + qsnorm(1 - alpha / 2) * diffS)
		}
    /* If 0 is within the confidence interval, we conclude that there is no
    statiscal difference between the two alternatives */
		CI._1 <= 0 && 0 <= CI._2
	}

	/** ANOVA separates the total variation in a set of measurements into a component due to random fluctuations
	 *  in the measurements and a component due to the actual differences among the alternatives.
	 *  
	 *  If the variation between the alternatives is larger than the variation within each alternative, then
	 *  it can be concluded that there is a statistically significant difference between the alternatives.
	 *  
	 *  For more information see: Statistically Rigorous Java Performance Evaluation, Andy Georges, Dries Buytaert, Lieven Eeckhout
	 */
	def ANOVAFTest(history: Seq[Seq[Long]], alpha: Double): Boolean = {
		val alternatives = history
		val means: Seq[Double] = for(a <- alternatives) yield mean(a)
		val overallMean: Double = means.reduceLeft(_ + _) / means.length

		val SSA = (means zip history.map(_.length)).foldLeft(0.0) { (sum: Double, p: (Double, Int)) =>
			val yi = p._1
			val ni = p._2
			sum + ni * (yi - overallMean) * (yi - overallMean)
	  }

		/* Computation of SSE */
		val doubleSumTerms = for ((alternative, mean) <- alternatives zip means; yij <- alternative) yield (yij - mean) * (yij - mean)
		val SSE = doubleSumTerms reduceLeft (_ + _)

		val K = alternatives.length
		val N = alternatives.foldLeft(0)(_ + _.size)
		val F = SSA / SSE * (N - K) / (K - 1)

		F <= qf(1 - alpha, K - 1, N - K)
	}

	/** Compares the coefficient of variance to some `threshold` value.
	 *  
	 *  This heuristic can be used to detect if the measurement has stabilized.
	 */
	def CoV(measurements: Seq[Long], threshold: Double): Boolean = {
		val cov = stdev(measurements) / mean(measurements)
		cov <= threshold
	}

	/** Computes the mean of the sequence of measurements. */
	def mean(seq : Seq[Long]): Double = seq.sum * 1.0 / seq.length

	/** The sample standard sample deviation. It is the square root of S², unbiased estimator for the variance.
	 */
	def stdev(seq: Seq[Long]): Double = {
		val xbar = mean(seq)
		val squaresum: Double = seq.foldLeft(0.0)((sum, xi) => sum + (xi - xbar) * (xi - xbar))
		sqrt(squaresum / (seq.length - 1))
	}

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













