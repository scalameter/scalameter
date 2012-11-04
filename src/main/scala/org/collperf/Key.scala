package org.collperf






object Key {

  val verbose = "verbose"

  object dsl {
    val curve = "curve"
    val scope = "scope"
    val executor = "executor"
  }

  object machine {
    val jvmVersion = "jvm-version"
    val jvmVendor = "jvm-vendor"
    val jvmName = "jvm-name"
    val osName = "os-name"
    val osArch = "os-arch"
    val cores = "cores"
    val hostname = "hostname"
  }

  object gen {
    val unit = "unit"
  }

  object reporting {
    val startDate = "date-start"
    val endDate = "date-end"
    val bigO = "big-o"
    val resultDir = "result-dir"

    object regression {
      val significance = "significance"
      val timeIndices = "time-indices"
    }
  }

  object exec {
    val benchRuns = "runs"
    val minWarmupRuns = "min-warmups"
    val maxWarmupRuns = "max-warmups"
    val independentSamples = "independent-samples"

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

