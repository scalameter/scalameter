package org.scalameter

import org.scalameter.picklers.Pickler


/* A mixin for keys whose values require pickler based serialization. */
trait PicklerBasedKey[T] extends Serializable {
  /** Name of the key that will be serialized. */
  def fullName: String

  /** Pickler used to deserialize value to which the key refers. */
  def pickler: Pickler[T]

  /** Indicates if a key should be skipped during serialization - if `true`, the key will not be serialized. */
  def isTransient: Boolean = false

  private[scalameter] final def repr: String = s"$fullName|${pickler.getClass.getName}"
}

object PicklerBasedKey {
  /** Reconstructs key from serialized string.
   *
   *  @param str serialized string
   *  @param constructor factory method to create specific key instance
   */
  def fromString[K <: PicklerBasedKey[_]](str: String, constructor: (String, Pickler[_]) => K): K = {
    val splitIdx = str.lastIndexOf('|')
    if (splitIdx == -1) sys.error("""Invalid key string. It should have following the form "fullName|picklerClass".""")
    val pickler = Pickler.makeInstance[Any](Class.forName(str.substring(splitIdx + 1)))
    val key = constructor(str.substring(0, splitIdx), pickler)
    key
  }
}
