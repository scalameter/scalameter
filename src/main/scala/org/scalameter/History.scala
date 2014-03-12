package org.scalameter

import java.util.Date

@SerialVersionUID(-2666789428423525666L)
case class History(results: Seq[History.Entry], infomap: Map[Key[_], Any] = Map.empty) {
  def info[T](key: Key[T], fallback: T) = infomap.getOrElse(key, fallback).asInstanceOf[T]
  def curves = results.map(_._3)
  def dates = results.map(_._1)

  override def toString = s"History(${results.mkString("\n")},\ninfo: $infomap)"
}

object History {
  type Entry = (Date, Context, CurveData)
}
