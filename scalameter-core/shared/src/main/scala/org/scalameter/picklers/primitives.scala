package org.scalameter.picklers

import java.nio.ByteBuffer


abstract class PrimitivePickler[T] extends Pickler[T] {
  protected def bits: Int

  protected def unwrap(from: ByteBuffer): T

  final protected def byteBuffer: ByteBuffer = ByteBuffer.allocate(numBytes)

  final def numBytes: Int = bits / java.lang.Byte.SIZE

  final def unpickle(a: Array[Byte], from: Int): (T, Int) = {
    val newFrom = if (from + numBytes == a.length) -1 else from + numBytes
    (unwrap(ByteBuffer.wrap(a, from, numBytes)), newFrom)
  }
}

object UnitPickler extends PrimitivePickler[Unit] {
  protected def bits: Int = 0

  protected def unwrap(from: ByteBuffer) = ()

  def pickle(x: Unit): Array[Byte] = Array.empty[Byte]
}

object BytePickler extends PrimitivePickler[Byte] {
  protected def bits: Int = java.lang.Byte.SIZE

  protected def unwrap(from: ByteBuffer) = from.get

  def pickle(x: Byte): Array[Byte] = byteBuffer.put(x).array()
}

object BooleanPickler extends PrimitivePickler[Boolean] {
  protected def bits: Int = java.lang.Byte.SIZE

  protected def unwrap(from: ByteBuffer) = {
    val v = from.get
    if (v == 1.toByte) true else if (v == 0.toByte) false else sys.error(s"Corrupted stream. Expected 0 or 1. Got $v")
  }

  def pickle(x: Boolean): Array[Byte] = byteBuffer.put(if (x) 1.toByte else 0.toByte).array()
}

object CharPickler extends PrimitivePickler[Char] {
  protected def bits: Int = java.lang.Character.SIZE

  protected def unwrap(from: ByteBuffer) = from.getChar

  def pickle(x: Char): Array[Byte] = byteBuffer.putChar(x).array()
}

object ShortPickler extends PrimitivePickler[Short] {
  protected def bits: Int = java.lang.Short.SIZE

  protected def unwrap(from: ByteBuffer) = from.getShort

  def pickle(x: Short): Array[Byte] = byteBuffer.putShort(x).array()
}

object IntPickler extends PrimitivePickler[Int] {
  protected def bits: Int = java.lang.Integer.SIZE

  protected def unwrap(from: ByteBuffer) = from.getInt

  def pickle(x: Int): Array[Byte] = byteBuffer.putInt(x).array()
}

object LongPickler extends PrimitivePickler[Long] {
  protected def bits: Int = java.lang.Long.SIZE

  protected def unwrap(from: ByteBuffer) = from.getLong

  def pickle(x: Long): Array[Byte] = byteBuffer.putLong(x).array()
}

object FloatPickler extends PrimitivePickler[Float] {
  protected def bits: Int = java.lang.Float.SIZE

  protected def unwrap(from: ByteBuffer) = from.getFloat

  def pickle(x: Float): Array[Byte] = byteBuffer.putFloat(x).array()
}

object DoublePickler extends PrimitivePickler[Double] {
  protected def bits: Int = java.lang.Double.SIZE

  protected def unwrap(from: ByteBuffer) = from.getDouble

  def pickle(x: Double): Array[Byte] = byteBuffer.putDouble(x).array()
}
