package org.collperf
package collections



import collection._



trait Collections extends PerformanceTest {

  /* data */

  def sizes(from: Int, to: Int, by: Int) = Gen.range("size")(from, to, by)

  def sized[T, Repr](g: Gen[Repr])(implicit ev: Repr <:< Traversable[T]): Gen[(Int, Repr)] = for (xs <- g) yield (xs.size, xs)

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

  def hashtriemaps(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    var hm = immutable.HashMap[Int, Int]()
    for (x <- 0 until size) hm += ((x, x))
    hm
  }

  def treemaps(from: Int, to: Int, by: Int) = for {
    size <- sizes(from, to, by)
  } yield {
    var am = immutable.TreeMap[Int, Int]()
    for (x <- 0 until size) am += ((x, x))
    am
  }

}



