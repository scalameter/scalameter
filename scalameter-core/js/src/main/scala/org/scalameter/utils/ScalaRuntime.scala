package org.scalameter.utils

import org.scalameter._

object ScalaRuntime extends AbstractScalaRuntime {

  def IS_OS_WINDOWS : Boolean = false
  
  def machineContext : Context = Context.empty
  
  def defaultClassPath : ClassPath = ClassPath(Seq())

}