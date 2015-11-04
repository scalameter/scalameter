package org.scalameter



import language.implicitConversions
import scala.collection.{Seq, immutable}
import org.scalameter.Key._



case class Context(properties: immutable.Map[Key[_], Any]) {
  def -[T](t: Key[T]) = Context(properties - t)
  def +[T](t: (Key[T], T)) = Context(properties + t)
  def ++(that: Context) = Context(this.properties ++ that.properties)
  def ++(that: Seq[KeyValue]) = Context(this.properties ++ that)
  def get[T](key: Key[T]): Option[T] =
    properties.get(key).asInstanceOf[Option[T]].orElse {
      key match {
        case k: KeyWithDefaultValue[_] => Some(k.default.asInstanceOf[T])
        case _ => None
      }
    }
  def goe[T](key: Key[T], v: => T) = properties.getOrElse(key, v).asInstanceOf[T]
  def apply[T](key: KeyWithDefault[T]) = {
    val value = key match {
      case k: KeyWithDefaultKey[_] =>
        properties.getOrElse(key, properties.getOrElse(k.default, k.default.default))
      case k: KeyWithDefaultValue[_] =>
        properties.getOrElse(k, k.default)
    }
    value.asInstanceOf[T]
  }
  def scope = scopeList.mkString(".")
  def scopeList = apply(dsl.scope).reverse
  def curve = apply(dsl.curve)
  override def toString = {
    val proplines = properties.map {
      case (key, value) => s"$key -> $value"
    } mkString("\n")
    s"Context(\n$proplines\n)"
  }
}


object Context {
  def apply(xs: KeyValue*) = new Context(xs.asInstanceOf[Seq[(Key[_], Any)]].toMap)

  val empty = new Context(immutable.Map.empty)

  val topLevel = machine ++ Context(
    preJDK7 -> false,
    dsl.scope -> Nil,
    exec.benchRuns -> 36,
    exec.minWarmupRuns -> 10,
    exec.maxWarmupRuns -> 50,
    exec.jvmflags -> List("-Xmx2048m", "-Xms2048m", "-XX:CompileThreshold=100"),
    classpath -> utils.ClassPath.default,
    reports.regression.significance -> 1e-10
  )

  val inlineBenchmarking = machine ++ Context(
    exec.benchRuns -> 1,
    exec.minWarmupRuns -> 10,
    exec.maxWarmupRuns -> 50,
    exec.requireGC -> false,
    verbose -> false
  )

  def machine = Context(
    Key.machine.jvm.version -> sys.props("java.vm.version"),
    Key.machine.jvm.vendor -> sys.props("java.vm.vendor"),
    Key.machine.jvm.name -> sys.props("java.vm.name"),
    Key.machine.osName -> sys.props("os.name"),
    Key.machine.osArch -> sys.props("os.arch"),
    Key.machine.cores -> Runtime.getRuntime.availableProcessors,
    Key.machine.hostname -> java.net.InetAddress.getLocalHost.getHostName
  )

  @deprecated(
    "This implicit will be removed in 0.6. Replace config(opts: _*) with config(opts).",
    "0.5")
  implicit def toKeyValues(ctx: Context): Seq[KeyValue] =
    ctx.properties.toSeq.asInstanceOf[Seq[KeyValue]]
}
