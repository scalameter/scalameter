package org.scalameter



import collection._
import org.scalameter.picklers.Pickler
import org.scalameter.picklers.Implicits._



/** Base class for ScalaMeter generators.
 *
 *  Generators create warmup and benchmark datasets for running benchmarks.
 *  They support neat combinator-style syntax for composing more complex
 *  generators out of simpler ones.
 */
abstract class Gen[T] extends Serializable {
  self =>

  def map[S](f: T => S): Gen[S] = new Gen[S] {
    def warmupset = for (x <- self.warmupset) yield f(x)
    def dataset = for (params <- self.dataset) yield params
    def generate(params: Parameters) = f(self.generate(params))
    def cardinality: Int = self.cardinality
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
    def cardinality: Int = {
      val c1 = self.cardinality
      val c2 = f(self.generate(self.dataset.next())).cardinality
      c1 * c2
    }
  }

  def cross[S](that: Gen[S]): Gen[(T, S)] = for {
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

    override def cardinality: Int = self.cardinality
  }

  def warmupset: Iterator[T]

  def dataset: Iterator[Parameters]

  def generate(params: Parameters): T

  def cardinality: Int

  def cached: Gen[T] = new Gen[T] {
    @transient lazy val cachedWarmupset = self.warmupset.toSeq
    @transient lazy val cachedDataset = self.dataset.map(p => (p, self.generate(p))).toMap

    def axes = cachedDataset.head._1.axisData.keys.toSet
    def warmupset = cachedWarmupset.iterator
    def dataset = self.dataset
    def generate(params: Parameters) = {
      val desiredParams = Parameters(params.axisData.filterKeys(k => axes.contains(k)).toSeq: _*)
      cachedDataset(desiredParams)
    }
    def cardinality: Int = Gen.this.cardinality
  }

}


object Gen {

  def unit(axisName: String): Gen[Unit] = new Gen[Unit] {
    def warmupset = Iterator.single(())
    def dataset = Iterator.single(Parameters((Parameter[Unit](axisName), ())))
    def generate(params: Parameters) = params[Unit](axisName)
    def cardinality: Int = 1
  }

  def single[T: Pickler](axisName: String)(v: T): Gen[T] = enumeration(axisName)(v)

  def range(axisName: String)(from: Int, upto: Int, hop: Int): Gen[Int] = new Gen[Int] {
    def warmupset = Iterator.single(upto)
    def dataset = (from to upto by hop).iterator.map(x => Parameters(Parameter[Int](axisName) -> x))
    def generate(params: Parameters) = params[Int](axisName)
    def cardinality: Int = math.max(1, (upto - from) / hop)
  }

  def enumeration[T: Pickler](axisName: String)(xs: T*): Gen[T] = new Gen[T] {
    def warmupset = Iterator.single(xs.last)
    def dataset = xs.iterator.map(x => Parameters(Parameter[T](axisName) -> x))
    def generate(params: Parameters) = params[T](axisName)
    def cardinality: Int = xs.size
  }

  def exponential(axisName: String)(from: Int, until: Int, factor: Int): Gen[Int] = new Gen[Int] {
    def warmupset = Iterator.single((until - from) / 2)
    def dataset = Iterator.iterate(from)(_ * factor).takeWhile(_ <= until).map(x => Parameters(Parameter[Int](axisName) -> x))
    def generate(params: Parameters) = params[Int](axisName)
    def cardinality: Int = math.max(1, math.log(until / from) / math.log(factor)).toInt
  }

  /* combinators */

  def listOfN[A: Pickler](axisName: String)(size: Int, gen: Gen[A]): Gen[List[A]] = {
    val initial = single(axisName)(List.empty[A])
    List.fill(size)(gen).foldRight(initial) { (gen, list) =>
      list.cross(gen).map { case (xs, x) => x :: xs }
    }
  }

  def crossProduct[P, Q](p: Gen[P], q: Gen[Q]): Gen[(P, Q)] = {
    for {
      pv <- p
      qv <- q
    } yield (pv, qv)
  }

  def crossProduct[P, Q, R](p: Gen[P], q: Gen[Q], r: Gen[R]): Gen[(P, Q, R)] = {
    for {
      pv <- p
      qv <- q
      rv <- r
    } yield (pv, qv, rv)
  }

  def crossProduct[P, Q, R, S](
    p: Gen[P], q: Gen[Q], r: Gen[R], s: Gen[S]
  ): Gen[(P, Q, R, S)] = {
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















