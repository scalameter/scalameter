package org.scalameter.picklers

import java.nio.ByteBuffer
import java.util.Date


object StringPickler extends Pickler[String] {
  def pickle(x: String): Array[Byte] = {
    val strBytes = x.getBytes("UTF-8")
    val buffer = ByteBuffer.allocate(IntPickler.numBytes + strBytes.length)
    buffer.putInt(strBytes.length)
    buffer.put(strBytes)
    buffer.array()
  }

  def unpickle(a: Array[Byte], from: Int): (String, Int) = {
    val (strLen, strFrom) = IntPickler.unpickle(a, from)
    val newFrom = if (strFrom + strLen == a.length) -1 else strFrom + strLen
    if (strFrom > 0) (new String(a, strFrom, strLen, "UTF-8"), newFrom)
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

object EnumPickler extends Pickler[java.lang.Enum[_]] {
  def pickle(x: Enum[_]): Array[Byte] = {
    val enumClazz = x.getDeclaringClass.getName
    val enumName = x.name()
    val buffer = ByteBuffer.allocate(
      IntPickler.numBytes + enumClazz.length + IntPickler.numBytes + enumName.length
    )
    buffer.put(StringPickler.pickle(enumClazz))
    buffer.put(StringPickler.pickle(enumName))
    buffer.array()
  }

  def unpickle(a: Array[Byte], from: Int): (Enum[_], Int) = {
    val (className, newFrom) = StringPickler.unpickle(a, from)
    val (enumName, pos) = StringPickler.unpickle(a, newFrom)
    val enumClass = Class.forName(className).asInstanceOf[Class[Enum[_]]]
    val enum = enumClass.getEnumConstants.find(_.toString == enumName)
      .getOrElse(sys.error("Corrupted stream. Expected java enum."))
    (enum, pos)
  }
}
