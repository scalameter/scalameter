package org.scalameter.utils



import org.scalatest.FunSuite



class SlidingWindowTest extends FunSuite {

  def newSlidingWindow(capacity: Int, size: Int) = {
    val sw = new SlidingWindow(capacity)
    for (i <- 0 until size) sw.add(i)
    sw
  }

  def testSize(capacity: Int, size: Int) {
    val sw = newSlidingWindow(capacity, size)
    assert(sw.size == math.min(size, capacity), (size, capacity, sw, sw.size))
  }

  def testSum(capacity: Int, size: Int) {
    val sw = newSlidingWindow(capacity, size)
    assert(sw.sum == size * (size - 1) / 2, (size * (size - 1) / 2, sw, sw.sum))
  }

  def testSumOverflow(capacity: Int, size: Int) {
    val sw = newSlidingWindow(capacity, size)
    val expected = 
      if (size > capacity) ((size - capacity) until size).sum
      else size * (size - 1) / 2
    assert(sw.sum == expected, (capacity, size, expected, sw, sw.sum))
  }

  def testIterator(capacity: Int, size: Int) {
    val sw = newSlidingWindow(capacity, size)
    if (size > capacity) assert(((size - capacity) until size) sameElements sw.iterator.toList, (capacity, size, sw, sw.iterator.toList))
    else assert((0 until size) sameElements sw.iterator.toList, (capacity, size, sw, sw.iterator.toList, sw.iterator))
  }

  test("SlidingWindow.size") {
    for {
      capacity <- 1 until 20
      size <- 0 until 20
    } testSize(capacity, size)
  }

  test("SlidingWindow.sum") {
    for {
      capacity <- 1 until 20
      size <- 0 until capacity
    } testSum(capacity, size)

    for {
      capacity <- 1 until 20
      size <- 0 until 20
    } testSumOverflow(capacity, size)
  }

  test("SlidingWindow.iterator") {
    for {
      capacity <- 1 until 20
      size <- 0 until 20
    } testIterator(capacity, size)
  }

}



