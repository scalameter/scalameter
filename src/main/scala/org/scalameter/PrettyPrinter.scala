package org.scalameter


/** Pretty prints values.
 *
 *  The default implementation simply calls `toString` method.
 */
trait PrettyPrinter[T] extends Serializable {
  def prettyPrint(value: T): String
}

sealed trait LowPriorityImplicits {
  implicit def genericPrinter[T]: PrettyPrinter[T] = new PrettyPrinter[T] {
    def prettyPrint(value: T): String = value.toString
  }
}

object PrettyPrinter extends LowPriorityImplicits {
  object Implicits {
    implicit class Ops[T: PrettyPrinter](lhs: T) {
      def prettyPrint: String = implicitly[PrettyPrinter[T]].prettyPrint(lhs)
    }
  }

  implicit val doublePrinter: PrettyPrinter[Double] = new PrettyPrinter[Double] {
    def prettyPrint(value: Double): String = f"$value%.3f"
  }
}
