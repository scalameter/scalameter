package org.scalameter.picklers

import java.nio.ByteBuffer
import java.util.Date


object StringPickler extends Pickler[String] {
  def pickle(x: String): Array[Byte] = {
    val strBytes = x.getBytes
    val buffer = ByteBuffer.allocate(IntPickler.numBytes + strBytes.length)
    buffer.putInt(strBytes.length)
    buffer.put(strBytes)
    buffer.array()
  }

  def unpickle(a: Array[Byte], from: Int): (String, Int) = {
    val (strLen, strFrom) = IntPickler.unpickle(a, from)
    val newFrom = if (strFrom + strLen == a.length) -1 else strFrom + strLen
    if (strFrom > 0) (new String(a, strFrom, strLen), newFrom)
    else ("", -1)
  }
}

object DatePickler extends Pickler[Date] {
  def pickle(x: Date): Array[Byte] = LongPickler.pickle(x.getTime)

  def unpickle(a: Array[Byte], from: Int): (Date, Int) = {
    val (obj, pos) = LongPickler.unpickle(a, from)
    (new Date(obj), pos)
  }
}
