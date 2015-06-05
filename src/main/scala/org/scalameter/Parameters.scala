package org.scalameter

import scala.language.existentials
import scala.collection.{Iterable, immutable}
import Parameters.P


@SerialVersionUID(4203959258570851398L)
case class Parameters(axisData: immutable.ListMap[Parameter[P], P]) {
  def ++(that: Parameters) = Parameters(this.axisData ++ that.axisData)
  def apply[T: Manifest](key: String) = axisData.apply(Parameter[T](key)).asInstanceOf[T]
  def map(f: (String, P) => (String, P)) = {
    val newAxisData = axisData.map { case (k, v) =>
      val n = f(k.name, v)
      (Parameter(n._1)(k.typeHint), n._2)
    }
    copy(axisData = newAxisData.asInstanceOf[immutable.ListMap[Parameter[_], Any]])
  }

  override def toString = s"Parameters(${axisData.map(t => t._1 + " -> " + t._2).mkString(", ")})"
}

object Parameters {
  private type P = T forSome { type T }

  def apply(xs: (Parameter[P], P)*) = new Parameters(immutable.ListMap(xs: _*))

  implicit val ordering = Ordering.by[Parameters, Iterable[String]] {
    _.axisData.toSeq.map(_._1.name).sorted.toIterable
  }
}
