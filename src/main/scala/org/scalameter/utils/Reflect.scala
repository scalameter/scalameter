package org.scalameter
package utils

object Reflect {

  def singletonInstance[C](module: Class[C]) =
    module.getField("MODULE$").get(null).asInstanceOf[Bench[_]]

}

