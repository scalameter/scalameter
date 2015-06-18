package org.scalameter.picklers

import java.nio.ByteBuffer


abstract class PrimitivePickler[T] extends Pickler[T] {
  protected def bits: Int

  final protected def byteBuffer: ByteBuffer = ByteBuffer.allocate(bytes)
  
  final def bytes: Int = bits / java.lang.Byte.SIZE
}

object BooleanPickler extends PrimitivePickler[Boolean] {
  protected def bits: Int = java.lang.Byte.SIZE

  def pickle(x: Boolean): Array[Byte] = byteBuffer.put(if (x) 1.toByte else 0.toByte).array()

  def unpickle(a: Array[Byte]): Boolean = if (ByteBuffer.wrap(a).get(0) == 0.toByte) false else true
}

object CharPickler extends PrimitivePickler[Char] {
  protected def bits: Int = java.lang.Character.SIZE

  def pickle(x: Char): Array[Byte] = byteBuffer.putChar(x).array()

  def unpickle(a: Array[Byte]): Char = ByteBuffer.wrap(a).getChar(0)
}

object ShortPickler extends PrimitivePickler[Short] {
  protected def bits: Int = java.lang.Short.SIZE

  def pickle(x: Short): Array[Byte] = byteBuffer.putShort(x).array()

  def unpickle(a: Array[Byte]): Short = ByteBuffer.wrap(a).getShort(0)
}

object IntPickler extends PrimitivePickler[Int] {
  protected def bits: Int = java.lang.Integer.SIZE

  def pickle(x: Int): Array[Byte] = byteBuffer.putInt(x).array()

  def unpickle(a: Array[Byte]): Int = ByteBuffer.wrap(a).getInt(0)
}

object LongPickler extends PrimitivePickler[Long] {
  protected def bits: Int = java.lang.Long.SIZE

  def pickle(x: Long): Array[Byte] = byteBuffer.putLong(x).array()

  def unpickle(a: Array[Byte]): Long = ByteBuffer.wrap(a).getLong(0)
}

object FloatPickler extends PrimitivePickler[Float] {
  protected def bits: Int = java.lang.Float.SIZE

  def pickle(x: Float): Array[Byte] = byteBuffer.putFloat(x).array()

  def unpickle(a: Array[Byte]): Float = ByteBuffer.wrap(a).getFloat(0)
}

object DoublePickler extends PrimitivePickler[Double] {
  protected def bits: Int = java.lang.Double.SIZE

  def pickle(x: Double): Array[Byte] = byteBuffer.putDouble(x).array()

  def unpickle(a: Array[Byte]): Double = ByteBuffer.wrap(a).getDouble(0)
}
