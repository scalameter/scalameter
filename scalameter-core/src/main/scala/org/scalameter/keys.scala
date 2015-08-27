package org.scalameter



import java.io.File
import java.util.Date
import org.scalameter.execution.invocation.instrumentation.MethodSignature
import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.Pickler
import org.scalameter.utils.ClassPath
import scala.annotation.tailrec
import scala.collection._



class Key[T: Pickler](val name: String)(implicit container: KeyContainer)
extends PicklerBasedKey[T] {
  container.keys(name) = this

  override def toString = name
  override def hashCode = name.hashCode
  override def equals(x: Any) = x match {
    case k: Key[_] => name == k.name
    case _ => false
  }

  val fullName: String = {
    @tailrec def context(keyContainer: KeyContainer, acc: List[String] = Nil):
      String = {
      if (keyContainer == null || keyContainer.containerName.isEmpty) acc.mkString(".")
      else context(keyContainer.enclosing, keyContainer.containerName :: acc)
    }

    val ctx = context(container)
    if (ctx.isEmpty) name
    else s"$ctx.$name"
  }

  val pickler: Pickler[T] = implicitly[Pickler[T]]
}

/** Base for keys that have some kind of default value. */
class KeyWithDefault[T: Pickler](name: String)(implicit container: KeyContainer) extends Key[T](name)

/** Key that defaults to [[default]] if value under key is not found in a context.
 *
 *  Note that this key type is handled in [[org.scalameter.Context.apply)]].
 */
class KeyWithDefaultValue[T: Pickler](name: String, val default: T)
  (implicit container: KeyContainer)
extends KeyWithDefault[T](name)

/** Key that chains finding default value to [[KeyWithDefaultValue]]
 *  if value under key is not found in a context.
 * 
 *  Note that this key type is handled in [[org.scalameter.Context.apply)]].
 */
class KeyWithDefaultKey[T: Pickler](name: String, val default: KeyWithDefaultValue[T])
  (implicit container: KeyContainer)
extends KeyWithDefault[T](name)


object Key extends Keys {
  implicit val ordering: Ordering[Key[_]] = Ordering.by(_.name)
}


abstract class KeyContainer(val containerName: String, val enclosing: KeyContainer) {
  private[scalameter] val subs = mutable.Map[String, KeyContainer]()
  private[scalameter] val keys = mutable.Map[String, Key[_]]()

  implicit def container: KeyContainer = this

  if (enclosing != null) enclosing.subs(containerName) = this

  def parseKey(keyName: String): Key[_] = {
    val parts = keyName.split("\\.")
    parseKeyRecursive(keyName, parts.toList)
  }

  private[scalameter] def parseKeyRecursive(fullname: String, keyParts: List[String]):
    Key[_] = keyParts match {
    case name :: Nil =>
      // println(containerName, subs, keys, keyParts)
      keys(name)
    case name :: remaining =>
      // println(containerName, subs, keys, keyParts)
      subs(name).parseKeyRecursive(fullname, remaining)
    case Nil =>
      sys.error(s"no key with the given name: $fullname!")
  }
}


class Keys extends KeyContainer("", null) {

  def apply[T: Pickler](name: String)(implicit container: KeyContainer) =
    new Key[T](name)

  def apply[T: Pickler](name: String, defaultValue: T)
    (implicit container: KeyContainer) =
    new KeyWithDefaultValue[T](name, defaultValue)

  // Note: predefined keys need to be lazy
  // due to initialization order issue with object Key

  val verbose = apply[Boolean]("verbose", true)
  val classpath = apply[ClassPath]("classpath", ClassPath.default)
  val preJDK7 = apply[Boolean]("preJDK7", false)
  val scopeFilter = apply[String]("scopeFilter", "")

  private[scalameter] val finalClasspath =
    new KeyWithDefaultKey[ClassPath]("finalClasspath", classpath) {
      override def isTransient: Boolean = true
    }

  object dsl extends KeyContainer("dsl", Keys.this) {
    val curve = apply[String]("curve", "")
    val scope = apply[List[String]]("scope", Nil)
  }

  object machine extends KeyContainer("machine", Keys.this) {
    object jvm extends KeyContainer("jvm", machine) {
      val version = apply[String]("version")
      val vendor = apply[String]("vendor")
      val name = apply[String]("name")
    }

    val osName = apply[String]("osName")
    val osArch = apply[String]("osArch")
    val cores = apply[Int]("cores")
    val hostname = apply[String]("hostname")
  }

  object gen extends KeyContainer("gen", Keys.this) {
    val unit = apply[String]("unit")
  }

  object reports extends KeyContainer("reports", Keys.this) {
    val startDate = apply[Option[Date]]("startDate", None)
    val endDate = apply[Option[Date]]("endDate", None)
    val bigO = apply[String]("bigO")
    val resultDir = apply[String]("resultDir", "target/benchmarks")
    val colors = apply[Boolean]("colors", true)

    object regression extends KeyContainer("regression", reports) {
      val significance = apply[Double]("significance", 1e-10)
      val timeIndices = apply[Seq[Long]]("timeIndices")
      val noiseMagnitude = apply[Double]("noiseMagnitude", 0.0)
    }
  }

  object exec extends KeyContainer("exec", Keys.this) {
    val benchRuns = apply[Int]("benchRuns", 30)
    val minWarmupRuns = apply[Int]("minWarmupRuns", 10)
    val maxWarmupRuns = apply[Int]("maxWarmupRuns", 10)
    val warmupCovThreshold = apply[Double]("warmupCovThreshold", 0.1)
    val independentSamples = apply[Int]("independentSamples", 9)
    val jvmflags = apply[List[String]]("jvmflags", Nil)
    val jvmcmd = apply[String]("jvmcmd", "java")
    val requireGC = apply[Boolean]("requireGC", false)
    val assumeDeterministicRun = apply[Boolean]("assumeDeterministicRun", true)

    object reinstantiation extends KeyContainer("reinstantiation", exec) {
      val frequency = apply[Int]("frequency")
      val fullGC = apply[Boolean]("fullGC")
    }

    object outliers extends KeyContainer("outliers", exec) {
      val suspectPercent = apply[Int]("suspectPercent", 25)
      val covMultiplier = apply[Double]("covMultiplier", 2.0)
      val retries = apply[Int]("retries", 8)
    }

    object noise extends KeyContainer("noise", exec) {
      val magnitude = apply[Double]("magnitude", 0.0)
    }

    object measurers extends KeyContainer("measurers", exec) {
      import picklers.noPickler._

      private[scalameter] val methodInvocationLookupTable =
        new Key[mutable.ArrayBuffer[MethodSignature]]("methodInvocationLookupTable") {
          override def isTransient: Boolean = true
        }
      private[scalameter] val instrumentedJarPath =
        new Key[File]("instrumentedJarPath") {
          override def isTransient: Boolean = true
        }
    }
  }

  dsl
  machine
  machine.jvm
  gen
  reports
  reports.regression
  exec
  exec.reinstantiation
  exec.outliers
  exec.noise

}
