package org.scalameter

trait Foreach[T] {
  def foreach[U](f: T => U): Unit
}
