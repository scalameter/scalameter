package org.scalameter

object KeyValue {
  implicit def apply[T](p: (Key[T], T)): KeyValue[T] = new KeyValue(p._1, p._2)
}
case class KeyValue[T](key: Key[T], value: T) {
  def pair: (Key[T], T) = key -> value
}