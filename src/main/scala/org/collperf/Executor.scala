package org.collperf



import collection._
import utils.Tree



trait Executor {

  def run[T](setups: Tree[Setup[T]]): Tree[CurveData]

}


object Executor {

  trait Factory[E <: Executor] {
    def apply(aggregator: Aggregator): E

    def min = apply(Aggregator.min)

    def max = apply(Aggregator.max)

    def average = apply(Aggregator.average)

    def median = apply(Aggregator.average)

    def complete(a: Aggregator) = apply(Aggregator.complete(a))
  }

}


