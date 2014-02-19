package org.scalameter

import scala.util.DynamicVariable

class MonadicDynVar[T](v: T) extends DynamicVariable(v) {
  def using(nv: T) = new Foreach[Unit] {
    def foreach[U](f: Unit => U): Unit = withValue(nv)(f(()))
  }
}
