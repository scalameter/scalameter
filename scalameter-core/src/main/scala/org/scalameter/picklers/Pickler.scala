package org.scalameter.picklers

import scala.annotation.implicitNotFound


@implicitNotFound(msg ="""No Pickler available for ${T}. Please define yours or
import org.scalameter.picklers.noPickler._ and use SerializationPersistor.""")
abstract class Pickler[T] extends Serializable {
  def pickle(x: T): Array[Byte]

  def unpickle(a: Array[Byte], from: Int): (T, Int)

  final def unpickle(a: Array[Byte]): T = {
    val (obj, newPos) = unpickle(a, 0)
    if (newPos > 0) sys.error(s"Malformed data. Input: ${a.toVector}. Remaining: ${a.toVector.slice(newPos, a.length)}, $newPos")
    obj
  }
}

object Pickler {
  /** Makes instance of Pickler that can be either a scala object or a plain class.
   */
  def makeInstance[T](clazz: Class[_]): Pickler[T] = {
    if (clazz.getName.endsWith("$")) clazz.getField("MODULE$").get(null).asInstanceOf[Pickler[T]]
    else clazz.newInstance().asInstanceOf[Pickler[T]]
  }
}
