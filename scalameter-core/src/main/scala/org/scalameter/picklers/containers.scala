package org.scalameter.picklers

import scala.language.higherKinds
import scala.collection.generic.CanBuildFrom
import java.util.Date
import std._


abstract class TraversablePickler[C[_] <: Traversable[_], T: Pickler](implicit cbf: CanBuildFrom[Nothing, T, C[T]]) extends Pickler[C[T]] {
  def pickle(x: C[T]): Array[Byte] = {
    val pickler = implicitly[Pickler[T]].asInstanceOf[Pickler[Any]]
    val builder = Array.newBuilder[Byte]
    builder ++= IntPickler.pickle(x.size)
    x.foreach { e: Any =>
      builder ++= pickler.pickle(e)
    }
    builder.result()
  }

  def unpickle(a: Array[Byte]): C[T] = {
    val pickler = implicitly[Pickler[T]]
    val (sizeArray, elemArray) = a.splitAt(IntPickler.bytes)
    val size = IntPickler.unpickle(sizeArray)
    elemArray.grouped(elemArray.length / size).map(pickler.unpickle).to[C]
  }
}

abstract class OptionPickler[T: Pickler] extends Pickler[Option[T]] {
  def pickle(x: Option[T]): Array[Byte] = {
    x match {
      case Some(e) => implicitly[Pickler[T]].pickle(e)
      case None => Array.empty[Byte]
    }
  }

  def unpickle(a: Array[Byte]): Option[T] = {
    if (a.isEmpty) None
    else Some(implicitly[Pickler[T]].unpickle(a))
  }
}

object StringListPickler extends TraversablePickler[List, String]

object LongSeqPickler extends TraversablePickler[Seq, Long]

object DateOptionPickler extends OptionPickler[Date]
