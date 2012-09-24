package org.collperf



import collection._



trait Gen[T] {
  self =>

  def map[S](f: T => S): Gen[S] = new Gen[S] {
    def warmupset = for (x <- self.warmupset) yield f(x)
    def dataset = for ((x, params) <- self.dataset) yield (f(x), params)
  }

  def flatMap[S](f: T => Gen[S]): Gen[S] = new Gen[S] {
    def warmupset = for {
      x <- self.warmupset
      y <- f(x).warmupset
    } yield y
    def dataset = for {
      (x, selfparams) <- self.dataset
      (y, thatparams) <- f(x).dataset
    } yield (y, selfparams ++ thatparams)
  }

  def warmupset: TraversableOnce[T]

  def dataset: TraversableOnce[(T, Parameters)]

}


object Gen {

  def range(axisName: String)(from: Int, until: Int, step: Int): Gen[Int] = new Gen[Int] {
    def warmupset = Iterator.single(until)
    def dataset = Iterator.range(from, until, step).map(x => (x, Parameters(axisName -> x)))
  }

  def enumeration[T](axisName: String)(xs: T*): Gen[T] = new Gen[T] {
    def warmupset = Iterator.single(xs.last)
    def dataset = xs.iterator.map(x => (x, Parameters(axisName -> x)))
  }

  def exponential(axisName: String)(from: Int, until: Int, factor: Int): Gen[Int] = new Gen[Int] {
    def warmupset = Iterator.single((until - from) / 2)
    def dataset = Iterator.iterate(from)(_ * factor).takeWhile(_ < until).map(x => (x, Parameters(axisName -> x)))
  }

}