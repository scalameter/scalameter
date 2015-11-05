package org.scalameter

import scala.collection.mutable


object MethodCounter {
  private val counter = mutable.Map.empty[String, Int]

  def reset(): Unit = {
    counter.clear()
  }

  def counts: Map[String, Int] = counter.toMap

  def called(method: String): Unit = {
    counter.synchronized {
      counter += (method -> (counter.getOrElse(method, 0) + 1))
    }
  }
}
