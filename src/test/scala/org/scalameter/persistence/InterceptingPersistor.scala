package org.scalameter
package persistence

import java.io.File

import scala.collection.mutable


/** Intercepts [[org.scalameter.History]] before serializing it
 *  using underlying [[org.scalameter.persistence.IOStreamPersistor]] to allow testing of correctness.
 */
class InterceptingPersistor(underlying: IOStreamPersistor[_, _]) extends Persistor {
  private val _cache: mutable.Map[Context, History[_]] = mutable.Map.empty

  def fileFor(ctx: Context): File = underlying.fileFor(ctx)

  def load[T](context: Context): History[T] = {
    underlying.load(context)
  }

  def save[T](context: Context, h: History[T]): Unit = synchronized {
    _cache += context -> h
    underlying.save(context, h)
  }

  def intercepted: Iterator[(Context, History[_])] = _cache.iterator
}
