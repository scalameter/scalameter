package org.scalameter

import scala.collection.{Iterable, immutable}

@SerialVersionUID(4203959258570851398L)
case class Parameters(axisData: immutable.ListMap[String, Any]) {
  def ++(that: Parameters) = Parameters(this.axisData ++ that.axisData)
  def apply[T](key: String) = axisData.apply(key).asInstanceOf[T]

  override def toString = s"Parameters(${axisData.map(t => t._1 + " -> " + t._2).mkString(", ")})"
}

object Parameters {
  def apply(xs: (String, Any)*) = new Parameters(immutable.ListMap(xs: _*))

  implicit val ordering = Ordering.by[Parameters, Iterable[String]] {
    _.axisData.toSeq.map(_._1).sorted.toIterable
  }
}
