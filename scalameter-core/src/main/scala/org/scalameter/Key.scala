package org.scalameter

import java.util.Date


class Key[T](val name: String) extends Serializable {
  override def toString = name
  override def hashCode = name.hashCode
  override def equals(x: Any) = x match {
    case k: Key[_] => name == k.name
    case _ => false
  }
}

class KeyWithDefault[T](name: String, val defaultValue: T) extends Key[T](name)


object Key extends Keys {
  def apply[T](name: String) = new Key[T](name)
  def apply[T](name: String, defaultValue: T) = new KeyWithDefault[T](name, defaultValue)

  implicit val ordering: Ordering[Key[_]] = Ordering.by(_.name)
}


class Keys {

  // Note: predefined keys need to be lazy
  // due to initialization order issue with object Key

  lazy val verbose = Key[Boolean]("verbose", true)
  lazy val classpath = Key[String]("classpath")
  lazy val preJDK7 = Key[Boolean]("pre-jdk-7", false)
  lazy val scopeFilter = Key[String]("scope-filter", "")

  object dsl {
    lazy val curve = Key[String]("curve", "")
    lazy val scope = Key[List[String]]("scope", Nil)
  }

  object machine {
    object jvm {
      lazy val version = Key[String]("jvm-version")
      lazy val vendor = Key[String]("jvm-vendor")
      lazy val name = Key[String]("jvm-name")
    }

    lazy val osName = Key[String]("os-name")
    lazy val osArch = Key[String]("os-arch")
    lazy val cores = Key[Int]("cores")
    lazy val hostname = Key[String]("hostname")
  }

  object gen {
    lazy val unit = Key[String]("unit")
  }

  object reports {
    lazy val startDate = Key[Option[Date]]("date-start", None)
    lazy val endDate = Key[Option[Date]]("date-end", None)
    lazy val bigO = Key[String]("big-o")
    lazy val resultDir = Key[String]("result-dir", "tmp")
    lazy val colors = Key[Boolean]("colors", true)

    object regression {
      lazy val significance = Key[Double]("significance", 1e-10)
      lazy val timeIndices = Key[Seq[Long]]("time-indices")
      lazy val noiseMagnitude = Key[Double]("regression-noise-magnitude", 0.0)
    }
  }

  object exec {
    lazy val benchRuns = Key[Int]("runs", 10)
    lazy val minWarmupRuns = Key[Int]("min-warmups", 10)
    lazy val maxWarmupRuns = Key[Int]("max-warmups", 10)
    lazy val warmupCovThreshold = Key[Double]("cov-warmup", 0.1)
    lazy val independentSamples = Key[Int]("independent-samples", 9)
    lazy val jvmflags = Key[String]("jvm-flags", "")
    lazy val jvmcmd = Key[String]("jvm-cmd", "java -server")

    object reinstantiation {
      lazy val frequency = Key[Int]("frequency")
      lazy val fullGC = Key[Boolean]("full-gc")
    }

    object outliers {
      lazy val suspectPercent = Key[Int]("suspect-percent", 25)
      lazy val covMultiplier = Key[Double]("cov-multiplier", 2.0)
      lazy val retries = Key[Int]("outlier-retries", 8)
    }

    object noise {
      lazy val magnitude = Key[Double]("noise-magnitude", 0.0)
    }
  }

}
