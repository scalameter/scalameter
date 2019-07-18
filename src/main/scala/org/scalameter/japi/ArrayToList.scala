package org.scalameter.japi

object ArrayToList {

  def apply[A <: AnyRef](array: Array[A]): List[A] = array.toList

}
