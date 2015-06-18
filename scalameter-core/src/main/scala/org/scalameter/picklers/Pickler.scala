package org.scalameter.picklers

import scala.annotation.implicitNotFound


@implicitNotFound(msg ="""No Pickler available for ${T}. Please define yours or
import org.scalameter.picklers.nopickler._ and use SerializationPersistor.""")
abstract class Pickler[T] {
  def pickle(x: T): Array[Byte]

  def unpickle(a: Array[Byte]): T
}
