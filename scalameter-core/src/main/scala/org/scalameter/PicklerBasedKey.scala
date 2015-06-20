package org.scalameter

import org.scalameter.picklers.Pickler


/* A mixin for keys whose values require pickler based serialization. */
trait PicklerBasedKey[T] extends Serializable {
  /* Name of the key that will be serialized. */
  def fullName: String

  /* Pickler used to deserialize value to which the key refers. */
  def pickler: Pickler[T]

  private[scalameter] final def repr: String = s"$fullName|${pickler.getClass.getName}"
}
