package org.scalameter


case class Quantity[V](value: V, units: String) {
  override def toString: String = s"$value $units"
}
