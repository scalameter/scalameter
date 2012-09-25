package org



import java.util.Date
import collection._



package object collperf {

}


package collperf {

  object Key {
    val module = "module"
    val method = "method"
    val jvmVersion = "jvm-version"
    val jvmVendor = "jvm-vendor"
    val jvmName = "jvm-name"
    val osName = "os-name"
    val osArch = "os-arch"
    val cores = "cores"
    val hostname = "hostname"
  }

  case class Context(properties: immutable.Map[String, Any]) {
    def +(t: (String, Any)) = Context(properties + t)
  }

  object Context {
    val empty = Context(immutable.Map())

    val topLevel = machine

    def machine = Context(immutable.Map(
      Key.jvmVersion -> sys.props("java.vm.version"),
      Key.jvmVendor -> sys.props("java.vm.vendor"),
      Key.jvmName -> sys.props("java.vm.name"),
      Key.osName -> sys.props("os.name"),
      Key.osArch -> sys.props("os.arch"),
      Key.cores -> Runtime.getRuntime.availableProcessors,
      Key.hostname -> java.net.InetAddress.getLocalHost.getHostName
    ))
  }

  case class Parameters(axisData: immutable.ListMap[String, Any]) {
    def ++(that: Parameters) = Parameters(this.axisData ++ that.axisData)
  }

  object Parameters {
    def apply(xs: (String, Any)*) = new Parameters(immutable.ListMap(xs: _*))
  }

  case class Measurement(time: Long, params: Parameters)

  case class Result(measurements: Seq[Measurement], context: Context)

  case class History(results: Seq[(Date, Result)])

  case class Benchmark[T](context: Context, gen: Gen[T], setup: Option[T => Any], teardown: Option[T => Any], customwarmup: Option[() => Any], snippet: T => Any)

  trait Reporter {
    def report(result: Result, persistor: Persistor): Unit
  }

  object Reporter {
    object None extends Reporter {
      def report(result: Result, persistor: Persistor) {}
    }
  }

  trait Persistor {
    def load(context: Context): History
    def save(result: Result): Unit
  }

  object Persistor {
    object None extends Persistor {
      def load(context: Context): History = History(Nil)
      def save(result: Result) {}
    }
  }

}









