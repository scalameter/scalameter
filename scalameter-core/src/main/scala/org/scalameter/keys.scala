package org.scalameter



import java.util.Date
import scala.collection._



class Key[T](val name: String)(implicit container: KeyContainer) extends Serializable {
  container.keys(name) = this

  override def toString = name
  override def hashCode = name.hashCode
  override def equals(x: Any) = x match {
    case k: Key[_] => name == k.name
    case _ => false
  }
}


class KeyWithDefault[T](name: String, val defaultValue: T)(implicit container: KeyContainer)
extends Key[T](name)(container)


object Key extends Keys {
  def apply[T](name: String)(implicit container: KeyContainer) =
    new Key[T](name)
  def apply[T](name: String, defaultValue: T)(implicit container: KeyContainer) =
    new KeyWithDefault[T](name, defaultValue)
  implicit val ordering: Ordering[Key[_]] = Ordering.by(_.name)

}


abstract class KeyContainer(val containerName: String, val enclosing: KeyContainer) {
  private[scalameter] val subs = mutable.Map[String, KeyContainer]()
  private[scalameter] val keys = mutable.Map[String, Key[_]]()

  implicit def container: KeyContainer = this

  if (enclosing != null) enclosing.subs(containerName) = this

  def parseKey(keyName: String): Key[_] = {
    val parts = keyName.split(".")
    parseKeyRecursive(parts.toList)
  }

  private[scalameter] def parseKeyRecursive(keyParts: List[String]): Key[_] = keyParts match {
    case name :: Nil =>
      keys(name)
    case name :: remaining =>
      subs(name).parseKeyRecursive(remaining)
    case Nil =>
      sys.error("no key with the given name!")
  }
}


class Keys extends KeyContainer("", null) {

  // Note: predefined keys need to be lazy
  // due to initialization order issue with object Key

  lazy val verbose = Key[Boolean]("verbose", true)
  lazy val classpath = Key[String]("classpath")
  lazy val preJDK7 = Key[Boolean]("pre-jdk-7", false)
  lazy val scopeFilter = Key[String]("scope-filter", "")

  object dsl extends KeyContainer("dsl", Keys.this) {
    lazy val curve = Key[String]("curve", "")
    lazy val scope = Key[List[String]]("scope", Nil)
  }

  object machine extends KeyContainer("machine", Keys.this) {
    object jvm extends KeyContainer("jvm", machine) {
      lazy val version = Key[String]("jvm-version")
      lazy val vendor = Key[String]("jvm-vendor")
      lazy val name = Key[String]("jvm-name")
    }

    lazy val osName = Key[String]("os-name")
    lazy val osArch = Key[String]("os-arch")
    lazy val cores = Key[Int]("cores")
    lazy val hostname = Key[String]("hostname")
  }

  object gen extends KeyContainer("gen", Keys.this) {
    lazy val unit = Key[String]("unit")
  }

  object reports extends KeyContainer("reports", Keys.this) {
    lazy val startDate = Key[Option[Date]]("date-start", None)
    lazy val endDate = Key[Option[Date]]("date-end", None)
    lazy val bigO = Key[String]("big-o")
    lazy val resultDir = Key[String]("result-dir", "tmp")
    lazy val colors = Key[Boolean]("colors", true)

    object regression extends KeyContainer("regression", reports) {
      lazy val significance = Key[Double]("significance", 1e-10)
      lazy val timeIndices = Key[Seq[Long]]("time-indices")
      lazy val noiseMagnitude = Key[Double]("regression-noise-magnitude", 0.0)
    }
  }

  object exec extends KeyContainer("exec", Keys.this) {
    lazy val benchRuns = Key[Int]("runs", 10)
    lazy val minWarmupRuns = Key[Int]("min-warmups", 10)
    lazy val maxWarmupRuns = Key[Int]("max-warmups", 10)
    lazy val warmupCovThreshold = Key[Double]("cov-warmup", 0.1)
    lazy val independentSamples = Key[Int]("independent-samples", 9)
    lazy val jvmflags = Key[String]("jvm-flags", "")
    lazy val jvmcmd = Key[String]("jvm-cmd", "java -server")
    lazy val requireGC = Key[Boolean]("require-gc", false)

    object reinstantiation extends KeyContainer("reinstantiation", exec) {
      lazy val frequency = Key[Int]("frequency")
      lazy val fullGC = Key[Boolean]("full-gc")
    }

    object outliers extends KeyContainer("outliers", exec) {
      lazy val suspectPercent = Key[Int]("suspect-percent", 25)
      lazy val covMultiplier = Key[Double]("cov-multiplier", 2.0)
      lazy val retries = Key[Int]("outlier-retries", 8)
    }

    object noise extends KeyContainer("noise", exec) {
      lazy val magnitude = Key[Double]("noise-magnitude", 0.0)
    }
  }

}
