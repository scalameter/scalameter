package org.scalameter

object ScalaRuntime extends AbstractScalaRuntime {

  def machineContext = Context(
    Key.machine.jvm.version -> sys.props("java.vm.version"),
    Key.machine.jvm.vendor -> sys.props("java.vm.vendor"),
    Key.machine.jvm.name -> sys.props("java.vm.name"),
    Key.machine.osName -> sys.props("os.name"),
    Key.machine.osArch -> sys.props("os.arch"),
    Key.machine.cores -> Runtime.getRuntime.availableProcessors,
    Key.machine.hostname -> java.net.InetAddress.getLocalHost.getHostName)

}