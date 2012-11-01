package org.collperf
package collections



import collection._



trait Collections extends PerformanceTest {

  /* data */

  def sizes(from: Int, to: Int, by: Int) = Gen.range("size")(from, to, by)

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

}
