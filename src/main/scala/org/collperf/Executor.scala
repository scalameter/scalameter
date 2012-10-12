package org.collperf



import collection._
import compat._
import utils.Tree



trait Executor {

  def run[T](setups: Tree[Setup[T]]): Tree[CurveData]

}


object Executor {

  trait Factory[E <: Executor] {
    def apply(aggregator: Aggregator, m: Measurer): E

    def min = apply(Aggregator.min, new Measurer.Default)

    def max = apply(Aggregator.max, new Measurer.Default)

    def average = apply(Aggregator.average, new Measurer.Default)

    def median = apply(Aggregator.median, new Measurer.Default)

    def complete(a: Aggregator) = apply(Aggregator.complete(a), new Measurer.Default)
  }

  trait Measurer extends Serializable {
    def measure[U](measurements: Long, setup: () => Any, tear: () => Any, body: =>U): Seq[Long]
  }

  object Measurer {

    final class Default extends Measurer {
      def measure[U](measurements: Long, setup: () => Any, tear: () => Any, body: =>U): Seq[Long] = {
        var iteration = 0
        var times = List[Long]()
        while (iteration < measurements) {
          setup()

          val start = Platform.currentTime
          body
          val end = Platform.currentTime
          val time = end - start

          tear()

          times ::= time
          iteration += 1
        }
        times
      }
    }

  }

}


