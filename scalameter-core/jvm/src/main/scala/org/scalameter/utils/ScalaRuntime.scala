package org.scalameter.utils

import org.apache.commons.lang3.SystemUtils
import org.scalameter._

object ScalaRuntime extends AbstractScalaRuntime {

  def machineContext = Context(
    Key.machine.jvm.version -> sys.props("java.vm.version"),
    Key.machine.jvm.vendor -> sys.props("java.vm.vendor"),
    Key.machine.jvm.name -> sys.props("java.vm.name"),
    Key.machine.osName -> sys.props("os.name"),
    Key.machine.osArch -> sys.props("os.arch"),
    Key.machine.cores -> Runtime.getRuntime.availableProcessors,
    Key.machine.hostname -> java.net.InetAddress.getLocalHost.getHostName)

  def defaultClassPath : ClassPath = {
    ClassPath.extract(this.getClass.getClassLoader, sys.props("java.class.path"))
  }
  
   def IS_OS_WINDOWS : Boolean =  SystemUtils.IS_OS_WINDOWS


}