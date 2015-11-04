package java.lang

import scala.scalajs.js

class ClassLoader protected (parent: ClassLoader) {
  def this() = this(null)
  def getParent : ClassLoader = parent
}