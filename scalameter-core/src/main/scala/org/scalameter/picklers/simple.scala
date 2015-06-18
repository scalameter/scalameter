package org.scalameter.picklers

import java.util.Date


object StringPickler extends Pickler[String] {
  def pickle(x: String): Array[Byte] = x.getBytes

  def unpickle(a: Array[Byte]): String = new String(a)
}

object DatePickler extends Pickler[Date] {
  def pickle(x: Date): Array[Byte] = LongPickler.pickle(x.getTime)

  def unpickle(a: Array[Byte]): Date = new Date(LongPickler.unpickle(a))
}
