package org.scalameter.picklers

import scala.language.higherKinds
import scala.collection.generic.CanBuildFrom
import java.util.Date
import Implicits._


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

  def unpickle(a: Array[Byte], from: Int): (C[T], Int) = {
    val pickler = implicitly[Pickler[T]]
    val builder = cbf()

    @annotation.tailrec
    def _unpickle(times: Int, from: Int): (C[T], Int) = {
      if (times > 0 && from > 0) {
        val (obj, newFrom) = pickler.unpickle(a, from)
        builder += obj
        _unpickle(times - 1, newFrom)
      } else (builder.result(), from)
    }

    val (len, newFrom) = IntPickler.unpickle(a, from)
    _unpickle(len, newFrom)
  }
}

abstract class OptionPickler[T: Pickler] extends Pickler[Option[T]] {
  def pickle(x: Option[T]): Array[Byte] = {
    x match {
      case Some(e) => implicitly[Pickler[T]].pickle(e)
      case None => Array.empty[Byte]
    }
  }

  def unpickle(a: Array[Byte], from: Int): (Option[T], Int) = {
    if (a.isEmpty) (None, -1)
    else {
      val (obj, pos) = implicitly[Pickler[T]].unpickle(a, from)
      (Some(obj), pos)
    }
  }
}

object StringListPickler extends TraversablePickler[List, String]

object LongSeqPickler extends TraversablePickler[Seq, Long]

object DateOptionPickler extends OptionPickler[Date]
