package org.scalameter






class Key {

  val verbose = "verbose"
  val classpath = "classpath"
  val preJDK7 = "pre-jdk-7"
  val scopeFilter = "scope-filter"

  object dsl {
    val curve = "curve"
    val scope = "scope"
    val executor = "executor"
  }

  object machine {
    object jvm {
      val version = "jvm-version"
      val vendor = "jvm-vendor"
      val name = "jvm-name"
    }

    val osName = "os-name"
    val osArch = "os-arch"
    val cores = "cores"
    val hostname = "hostname"
  }

  object gen {
    val unit = "unit"
  }

  object reports {
    val startDate = "date-start"
    val endDate = "date-end"
    val bigO = "big-o"
    val resultDir = "result-dir"
    val colors = "false"

    object regression {
      val significance = "significance"
      val timeIndices = "time-indices"
      val noiseMagnitude = "regression-noise-magnitude"
    }
  }

  object exec {
    val benchRuns = "runs"
    val minWarmupRuns = "min-warmups"
    val maxWarmupRuns = "max-warmups"
    val warmupCovThreshold = "cov-warmup"
    val independentSamples = "independent-samples"
    val jvmflags = "jvm-flags"
    val jvmcmd = "jvm-cmd"

    object reinstantiation {
      val frequency = "frequency"
      val fullGC = "full-gc"
    }

    object outliers {
      val suspectPercent = "suspect-percent"
      val covMultiplier = "cov-multiplier"
      val retries = "outlier-retries"
    }

    object noise {
      val magnitude = "noise-magnitude"
    }
  }

}


object Key extends Key
