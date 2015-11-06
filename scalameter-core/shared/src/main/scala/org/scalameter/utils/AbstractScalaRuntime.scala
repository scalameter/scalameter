package org.scalameter.utils

import org.scalameter._

/**
 * Abstraction for all system properties that are dependent on the runtime
 */
trait AbstractScalaRuntime {
  
  def IS_OS_WINDOWS : Boolean
  
  def machineContext : Context 
  
  def defaultClassPath : ClassPath
}