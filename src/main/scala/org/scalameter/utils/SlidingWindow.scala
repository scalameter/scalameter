package org.scalameter.utils






final class SlidingWindow(_cap: Int) {
  require(_cap >= 1)

  val capacity = _cap + 1

  private val store = new Array[Double](capacity)
  private var first = 0
  private var next = 0

  def add(t: Double) {
    store(next) = t
    val inc = (next + 1) % capacity
    next = inc
    if (inc == first) first = (first + 1) % capacity
  }

  def size: Int = {
    if (first == next) 0
    else if (first < next) next - first
    else next + capacity - first
  }

  class Iterator extends scala.Iterator[Double] {
    var i = first
    def hasNext = i != SlidingWindow.this.next
    def next() = {
      val r = store(i)
      i = (i + 1) % capacity
      r
    }
    override def toString = s"SlidingWindow.Iterator(i: $i, sw: ${SlidingWindow.this})"
  }

  def iterator = new Iterator

  override def toString = s"SlidingWindow(first: $first, next: $next, raw: ${store.mkString(", ")})"

  def sum: Double = {
    var i = first
    var s = 0.0
    while (i != next) {
      s += store(i)
      i = (i + 1) % capacity
    }
    s
  }

  def mean: Double = sum.toDouble / size

  def stdev: Double = {
    val m = mean
    var i = first
    var s = 0.0
    while (i != next) {
      val diff = store(i) - m
      s += diff * diff
      i = (i + 1) % capacity
    }
    math.sqrt(s / (size - 1))
  }

  def cov: Double = stdev / mean

}