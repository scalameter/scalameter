package org.collperf



import collection._



trait Gen[T] extends Serializable {
  self =>

  def map[S](f: T => S): Gen[S] = new Gen[S] {
    def warmupset = for (x <- self.warmupset) yield f(x)
    def dataset = for (params <- self.dataset) yield params
    def generate(params: Parameters) = f(self.generate(params))
  }

  def flatMap[S](f: T => Gen[S]): Gen[S] = new Gen[S] {
    def warmupset = for {
      x <- self.warmupset
      y <- f(x).warmupset
    } yield y
    def dataset = for {
      selfparams <- self.dataset
      x = self.generate(selfparams)
      thatparams <- f(x).dataset
    } yield selfparams ++ thatparams
    def generate(params: Parameters) = {
      val x = self.generate(params)
      val mapped = f(x)
      mapped.generate(params)
    }
  }

  def warmupset: Iterator[T]

  def dataset: Iterator[Parameters]

  def generate(params: Parameters): T

}


object Gen {

  def unit: Gen[Unit] = new Gen[Unit] {
    def axisName = Key.gen.unit
    def warmupset = Iterator.single(unit)
    def dataset = Iterator.single(Parameters(axisName -> ()))
    def generate(params: Parameters) = params[Unit](axisName)
  }

  def single[T](axisName: String)(v: T): Gen[T] = enumeration(axisName)(v)

  def range(axisName: String)(from: Int, upto: Int, hop: Int): Gen[Int] = new Gen[Int] {
    def warmupset = Iterator.single(upto)
    def dataset = (from to upto by hop).iterator.map(x => Parameters(axisName -> x))
    def generate(params: Parameters) = params[Int](axisName)
  }

  def enumeration[T](axisName: String)(xs: T*): Gen[T] = new Gen[T] {
    def warmupset = Iterator.single(xs.last)
    def dataset = xs.iterator.map(x => Parameters(axisName -> x))
    def generate(params: Parameters) = params[T](axisName)
  }

  def exponential(axisName: String)(from: Int, until: Int, factor: Int): Gen[Int] = new Gen[Int] {
    def warmupset = Iterator.single((until - from) / 2)
    def dataset = Iterator.iterate(from)(_ * factor).takeWhile(_ <= until).map(x => Parameters(axisName -> x))
    def generate(params: Parameters) = params[Int](axisName)
  }

}















