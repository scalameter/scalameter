package org.scalameter



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

  def zip[S](that: Gen[S]): Gen[(T, S)] = for {
    x <- self
    y <- that
  } yield (x, y)

  def rename(mapping: (String, String)*): Gen[T] = new Gen[T] {
    val reverseMapping = mapping.map(kv => (kv._2, kv._1))
    def warmupset = self.warmupset
    def dataset = self.dataset.map(params => params map {
      case (k, v) => (mapping.toMap.applyOrElse(k, (k: String) => k), v)
    })
    def generate(params: Parameters) = self.generate(params map {
      case (k, v) => (reverseMapping.toMap.applyOrElse(k, (k: String) => k), v)
    })
  }

  def warmupset: Iterator[T]

  def dataset: Iterator[Parameters]

  def generate(params: Parameters): T

  def cached: Gen[T] = new Gen[T] {
    @transient lazy val cachedWarmupset = self.warmupset.toSeq
    @transient lazy val cachedDataset = self.dataset.map(p => (p, self.generate(p))).toMap

    def axes = cachedDataset.head._1.axisData.keys.toSet
    def warmupset = cachedWarmupset.iterator
    def dataset = cachedDataset.keys.iterator
    def generate(params: Parameters) = {
      val desiredParams = Parameters(params.axisData.filterKeys(k => axes.contains(k)).toSeq: _*)
      cachedDataset(desiredParams)
    }
  }

}


object Gen {

  def unit(axisName: String): Gen[Unit] = new Gen[Unit] {
    def warmupset = Iterator.single(())
    def dataset = Iterator.single(Parameters((axisName, ())))
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

  /* combinators */

  def tupled[P, Q](p: Gen[P], q: Gen[Q]): Gen[(P, Q)] = {
    for {
      pv <- p
      qv <- q
    } yield (pv, qv)
  }

  def tupled[P, Q, R](p: Gen[P], q: Gen[Q], r: Gen[R]): Gen[(P, Q, R)] = {
    for {
      pv <- p
      qv <- q
      rv <- r
    } yield (pv, qv, rv)
  }

  def tupled[P, Q, R, S](p: Gen[P], q: Gen[Q], r: Gen[R], s: Gen[S]): Gen[(P, Q, R, S)] = {
    for {
      pv <- p
      qv <- q
      rv <- r
      sv <- s
    } yield (pv, qv, rv, sv)
  }

  /** Provides most collection generators given that a size generator is defined.
   */
  trait Collections {

    def sizes: Gen[Int]

    /* sequences */

    def lists = for {
      size <- sizes
    } yield (0 until size).toList
  
    def arrays = for {
      size <- sizes
    } yield (0 until size).toArray
  
    def vectors = for {
      size <- sizes
    } yield (0 until size).toVector
   
    def arraybuffers = for {
      size <- sizes
    } yield mutable.ArrayBuffer(0 until size: _*)
  
    def ranges = for {
      size <- sizes
    } yield 0 until size
  
    /* maps */
  
    def hashtablemaps = for {
      size <- sizes
    } yield {
      val hm = mutable.HashMap[Int, Int]()
      for (x <- 0 until size) hm(x) = x
      hm
    }
   
  
    def linkedhashtablemaps = for {
      size <- sizes
    } yield {
      val hm = mutable.LinkedHashMap[Int, Int]()
      for (x <- 0 until size) hm(x) = x
      hm
    }
    
    def hashtriemaps = for {
      size <- sizes
    } yield {
      var hm = immutable.HashMap[Int, Int]()
      for (x <- 0 until size) hm += ((x, x))
      hm
    }
  
    def redblackmaps = for {
      size <- sizes
    } yield {
      var am = immutable.TreeMap[Int, Int]()
      for (x <- 0 until size) am += ((x, x))
      am
    }
  
    /* sets */
  
    def hashtablesets = for {
      size <- sizes
    } yield {
      val hs = mutable.HashSet[Int]()
      for (x <- 0 until size) hs.add(x)
      hs
    }
    
    def linkedhashtablesets = for {
      size <- sizes
    } yield {
      val hs = mutable.LinkedHashSet[Int]()
      for (x <- 0 until size) hs.add(x)
      hs
    }
    
    def avlsets = for {
      size <- sizes
    } yield {
      val as = mutable.TreeSet[Int]()
      for (x <- 0 until size) as.add(x)
      as
    }
  
    def redblacksets = for {
      size <- sizes
    } yield {
      var as = immutable.TreeSet[Int]()
      for (x <- 0 until size) as += x
      as
    }
  
    def hashtriesets = for {
      size <- sizes
    } yield {
      var hs = immutable.HashSet[Int]()
      for (x <- 0 until size) hs += x
      hs
    }

  }

}















