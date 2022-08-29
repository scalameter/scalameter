package org.scalameter.picklers



import language.higherKinds

import org.scalameter.picklers.Implicits._
import scala.annotation.tailrec
import scala.collection.compat._


abstract class IterablePickler[C[_] <: Iterable[_], T: Pickler]
extends Pickler[C[T]] {
  protected def factory: Factory[T, C[T]]

  def pickle(x: C[T]): Array[Byte] = {
    val pickler = implicitly[Pickler[T]].asInstanceOf[Pickler[Any]]
    val builder = Array.newBuilder[Byte]
    builder ++= IntPickler.pickle(x.size)
    x.iterator.foreach { e: Any =>
      builder ++= pickler.pickle(e)
    }
    builder.result()
  }

  def unpickle(a: Array[Byte], from: Int): (C[T], Int) = {
    val pickler = implicitly[Pickler[T]]
    val builder = factory.newBuilder

    @tailrec
    def unpickle(times: Int, from: Int): (C[T], Int) = {
      if (times > 0 && from > 0) {
        val (obj, newFrom) = pickler.unpickle(a, from)
        builder += obj
        unpickle(times - 1, newFrom)
      } else (builder.result(), from)
    }

    val (len, newFrom) = IntPickler.unpickle(a, from)
    unpickle(len, newFrom)
  }
}


class OptionPickler[T: Pickler] extends Pickler[Option[T]] {
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

class ListPickler[T: Pickler] extends IterablePickler[List, T] {
  protected def factory = implicitly[Factory[T, List[T]]]
}
class VectorPickler[T: Pickler] extends IterablePickler[Vector, T] {
  protected def factory = implicitly[Factory[T, Vector[T]]]
}

class SeqPickler[T: Pickler] extends IterablePickler[scala.collection.Seq, T] {
  protected def factory = implicitly[Factory[T, scala.collection.Seq[T]]]
}

object StringListPickler extends ListPickler[String]
