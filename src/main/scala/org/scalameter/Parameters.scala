package org.scalameter

import scala.collection.{Iterable, immutable}
import org.scalameter.picklers.noPickler.instance

@SerialVersionUID(4203959258570851399L)
case class Parameters(axisData: immutable.Map[Parameter[_], Any]) {
  def ++(that: Parameters) = Parameters(this.axisData ++ that.axisData)
  def apply[T](key: String) = axisData.apply(Parameter[T](key)).asInstanceOf[T]
  def map(f: ((String, Any)) => (String, Any)) = {
    Parameters(axisData.iterator.map { case (k, v) =>
      val n = f((k.fullName, v))
      (Parameter(n._1)(k.pickler).asInstanceOf[Parameter[_]], n._2)
    }.toSeq: _*)
  }

  override def toString =
    s"Parameters(${axisData.map{ case (p, v) => s"$p -> $v"}.mkString(", ")})"
}

object Parameters {
  def apply(xs: (Parameter[_], Any)*) = new Parameters(immutable.Map(xs: _*))

  implicit val ordering: Ordering[Parameters] = Ordering.by[Parameters, Iterable[String]] {
    _.axisData.toSeq.map(_._1.fullName).sorted.toIterable
  }
}
