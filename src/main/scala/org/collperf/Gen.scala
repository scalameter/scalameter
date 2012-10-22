package org.collperf



import collection._



trait Gen[T] extends Serializable {
  self =>

  def map[S](f: T => S): Gen[S] = new Gen[S] {
    def warmupset = for (x <- self.warmupset) yield f(x)
    def dataset = for (params <- self.dataset) yield params
    def regenerate(params: Parameters) = f(self.regenerate(params))
  }

  def flatMap[S](f: T => Gen[S]): Gen[S] = new Gen[S] {
    def warmupset = for {
      x <- self.warmupset
      y <- f(x).warmupset
    } yield y
    def dataset = for {
      selfparams <- self.dataset
      x = self.regenerate(selfparams)
      thatparams <- f(x).dataset
    } yield selfparams ++ thatparams
    def regenerate(params: Parameters) = {
      val x = self.regenerate(params)
      val mapped = f(x)
      mapped.regenerate(params)
    }
  }

  def warmupset: Iterator[T]

  def dataset: Iterator[Parameters]

  def regenerate(params: Parameters): T

}


object Gen {

  def unit: Gen[Unit] = new Gen[Unit] {
    def axisName = Key.unit
    def warmupset = Iterator.single(unit)
    def dataset = Iterator.single(Parameters(axisName -> ()))
    def regenerate(params: Parameters) = params[Unit](axisName)
  }

  def single[T](axisName: String)(v: T): Gen[T] = enumeration(axisName)(v)

  def range(axisName: String)(from: Int, upto: Int, hop: Int): Gen[Int] = new Gen[Int] {
    def warmupset = Iterator.single(upto)
    def dataset = (from to upto by hop).iterator.map(x => Parameters(axisName -> x))
    def regenerate(params: Parameters) = params[Int](axisName)
  }

  def enumeration[T](axisName: String)(xs: T*): Gen[T] = new Gen[T] {
    def warmupset = Iterator.single(xs.last)
    def dataset = xs.iterator.map(x => Parameters(axisName -> x))
    def regenerate(params: Parameters) = params[T](axisName)
  }

  def exponential(axisName: String)(from: Int, until: Int, factor: Int): Gen[Int] = new Gen[Int] {
    def warmupset = Iterator.single((until - from) / 2)
    def dataset = Iterator.iterate(from)(_ * factor).takeWhile(_ <= until).map(x => Parameters(axisName -> x))
    def regenerate(params: Parameters) = params[Int](axisName)
  }

}















