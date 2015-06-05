package org.scalameter
package persistence

import scala.collection.mutable


class InterceptingPersistor(underlying: org.scalameter.Persistor) extends Persistor {
  private val _cache: mutable.Map[Context, History] = mutable.Map.empty

  def load(context: Context): History = {
    underlying.load(context)
  }

  def save(context: Context, h: History): Unit = {
    _cache += context -> h
    underlying.save(context, h)
  }

  def cached: Iterator[(Context, History)] = _cache.iterator
}
