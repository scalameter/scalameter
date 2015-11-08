package org.scalameter
package collections



import collection._
import Key._



trait Collections {

  /* data */

  def sizes(from: Int, to: Int, by: Int) = Gen.range("size")(from, to, by)

  def sized[T, Repr](g: Gen[Repr])(implicit ev: Repr <:< Traversable[T]): Gen[(Int, Repr)] = for (xs <- g) yield (xs.size, xs)

  def sized[K, V](g: Gen[java.util.HashMap[K, V]]): Gen[(Int, java.util.HashMap[K, V])] = for (xs <- g) yield (xs.size, xs)

  /* sequences */

  def lists(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield (0 until size).toList

  def arrays(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield (0 until size).toArray

  def vectors(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield (0 until size).toVector

  def arraybuffers(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield mutable.ArrayBuffer(0 until size: _*)

  def ranges(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield 0 until size

  /* maps */

  def hashtablemaps(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    val hm = mutable.HashMap[Int, Int]()
    for (x <- 0 until size) hm(x) = x
    hm
  }

  def linkedhashtablemaps(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    val hm = mutable.LinkedHashMap[Int, Int]()
    for (x <- 0 until size) hm(x) = x
    hm
  }

  def juhashmaps(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    val hm = new java.util.HashMap[Int, Int]
    for (x <- 0 until size) hm.put(x, x)
    hm
  }

  def hashtriemaps(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    var hm = immutable.HashMap[Int, Int]()
    for (x <- 0 until size) hm += ((x, x))
    hm
  }

  def redblackmaps(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    var am = immutable.TreeMap[Int, Int]()
    for (x <- 0 until size) am += ((x, x))
    am
  }

  /* sets */

  def hashtablesets(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    val hs = mutable.HashSet[Int]()
    for (x <- 0 until size) hs.add(x)
    hs
  }

  def linkedhashtablesets(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    val hs = mutable.LinkedHashSet[Int]()
    for (x <- 0 until size) hs.add(x)
    hs
  }

  def avlsets(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    val as = mutable.TreeSet[Int]()
    for (x <- 0 until size) as.add(x)
    as
  }

  def redblacksets(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    var as = immutable.TreeSet[Int]()
    for (x <- 0 until size) as += x
    as
  }

  def hashtriesets(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    var hs = immutable.HashSet[Int]()
    for (x <- 0 until size) hs += x
    hs
  }

}



