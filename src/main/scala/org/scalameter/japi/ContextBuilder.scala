package org.scalameter.japi

import org.scalameter.{Context, Key}


/** Builder for the [[org.scalameter.Context]] that constructs key from the path.
 *
 *  Note that it's mainly useful for the Java users.
 *  Scala users can simply directly access key containers and keys.
 */
class ContextBuilder {
  private val mapBuilder = Map.newBuilder[Key[_], Any]

  def put(key: String, value: Any): this.type = {
    mapBuilder += (Key.parseKey(key) -> value)
    this
  }

  def build(): Context = Context(mapBuilder.result())
}
