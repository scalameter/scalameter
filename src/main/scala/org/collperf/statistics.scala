package org.collperf



import collection._
import math._
import org.apache.commons.math3.distribution.TDistribution
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.FDistribution


object Statistics {

	/**
	 * Let Y = (Y_1, ..., Y_n) data resulting from a parametric law F of
	 * scalar parameter θ. A confidence interval (B_i, B_s) is a statistic
	 * in the form of an interval containing θ with a specified probability.
	 */
	def CI(seq: Seq[Long], alpha: Double): (Double, Double) = {
		val n = seq.length
		val xbar = mean(seq)
		val S = sampleStandardDeviation(seq)
		/* Student's distribution could be used all the time because it converges
		 * towards the normal distribution as n grows.
		 */
		if (n < 30) {
			(xbar - qt(1 - alpha / 2, n - 1) * S / sqrt(n),
				xbar + qt(1 - alpha / 2, n - 1) * S / sqrt(n))
		} else {
			(xbar - qsnorm(1 - alpha / 2) * S / sqrt(n),
				xbar + qsnorm(1 - alpha / 2) * S / sqrt(n))
		}
	}

	// For two alternatives
	def CITest(alt1: Seq[Long], alt2: Seq[Long]): Boolean = {
		val diffM = mean(alt1) - mean(alt2)
		val S1 = sampleStandardDeviation(alt1)
		val S2 = sampleStandardDeviation(alt2)
		val n1 = alt1.length
		val n2 = alt2.length
		val diffS = sqrt(S1 * S1 / n1 + S2 * S2 / n2)
			var CI = (0.0, 0.0)
			if (n1 >= 30 && n2 >= 30) {
				CI = (diffM - qsnorm(1 - alpha / 2) * diffS,
					diffM + qsnorm(1 - alpha / 2) * diffS)
			} else {
				val ndf = math.round(pow(pow(S1, 2) / n1 + pow(S2, 2) / n2, 2) / (pow(pow(S1, 2) / n1, 2) / (n1 - 1) + pow(pow(S2, 2) / n2, 2) / (n2 - 1)))
				CI = (diffM - qt(1 - alpha / 2, ndf) * diffS,
					diffM + qt(1 - alpha / 2, ndf) * diffS)
			}
			/* If 0 is within the confidence interval, we conclude that there is no
			  statiscal difference between the two alternatives */
			return (!(CI._1 <= 0 && 0 <= CI._2))
	}

	/**
	 * ANOVA separates the total variation in a set of measurements into a component due to random fluctuations
	 * in the measurements and a component due to the actualdifferences among the alternatives. [...]
	 * If the variation between the alternatives is larger than the variation within each alternative, then
	 * it can be concluded that there is a statistically significant difference between the alternatives.
	 * Ref : Statistically Rigorous Java Performance Evaluation, Andy Georges, Dries Buytaert, Lieven Eeckhout
	 */
	def ANOVAFTest(history: Seq[Seq[Long]], newest: Seq[Long]): Boolean = {
		val alternatives = newest +: history
		val means = for(a <- alternatives) yield mean(a)
		val overallMean = means.reduceLeft(_ + _) / means.length
		// TODO : we should verify here that each alternative has the same number of measurements !
		val n = alternatives.head.length
		val SSA = n * (means.reduceLeft((sum: Long, t: Long) => sum + ((t - overallMean) * (t - overallMean))))

		/* Computation of SSE */
		val k = alternatives.length
		val doubleSumTerms = for(j <- 0 until k ; i <- 0 until n) yield pow(alternatives(j)(i) - means(j), 2);
		val SSE = doubleSumTerms reduceLeft (_ + _)

		val F = SSA * (k * (n - 1)) / (SSE * (k - 1))

		return (F > qf(1 - alpha, k - 1, k * (n - 1)))
	}

	def CoV(measurements: Seq[Long]): Boolean = {
		// val cov = sampleStandardDeviation(measurements) / mean(measurements)
		false // might be useful later
	}

	def mean(seq : Seq[Long]): Long = {
		if (seq.length == 0) 0 else seq reduceLeft(_ + _) / seq.length
	}

	/**
	 * The sample standard sample deviation. It is the square root of S², unbiased estimator for the variance.
	 */
	def sampleStandardDeviation(seq: Seq[Long]): Double = {
		val xbar = mean(seq)
		sqrt(seq.reduceLeft((sum: Long, xi: Long) => sum + ((xi - xbar) * (xi - xbar))) / (seq.length - 1))
	}

	/**
	 * Quantile function for the Student's t distribution.
	 * Let 0 < p < 1. The p-th quantile of the cumulative distribution function F(x) is defined as
	 * x_p = inf{x : F(x) >= p}
	 * For most of the continuous random variables, x_p is unique and is equal to x_p = F^(-1)(p), where
	 * F^(-1) is the inverse function of F. Thus, x_p is the value for which Pr(X <= x_p) = p. In particular,
	 * the 0.5-th quantile is called the median of F.
	 */
	private def qt(p: Double, df: Double): Double = {
		new TDistribution(df).inverseCumulativeProbability(p)
	}

	/**
	 * Quantile function for the standard (μ = 0, σ = 1) normal distribution
	 */
	private def qsnorm(p: Double): Double = {
		new NormalDistribution().inverseCumulativeProbability(p)
	}

	/**
	 * Quantile function for the F distribution
	 */
	private def qf(p: Double, df1: Double, df2: Double) = {
		new FDistribution(df1, df2).inverseCumulativeProbability(p)
	}

}