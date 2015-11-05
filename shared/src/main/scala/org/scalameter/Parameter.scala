package org.scalameter

import org.scalameter.picklers.Pickler


case class Parameter[T: Pickler](fullName: String) extends PicklerBasedKey[T] {
  val pickler: Pickler[T] = implicitly[Pickler[T]]

  override def hashCode(): Int = fullName.hashCode

  override def equals(x: Any): Boolean = x match {
    case p: Parameter[_] => fullName == p.fullName
    case _ => false
  }

  override def toString: String = fullName
}
