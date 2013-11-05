---
layout: default
title: Download
permalink: /download/index.html
---



## Direct download

The latest ScalaMeter release is **ScalaMeter 0.4** for Scala 2.10.


## Maven repository

ScalaMeter is available for download from Maven Central!

    <dependency>
      <groupId>com.github.axel22</groupId>
      <artifactId>scalameter_2.10</artifactId>
      <version>0.4</version>
    </dependency>

If you're using [SBT](/home/gettingstarted/sbt/), just add the following lines to `build.sbt`:

    libraryDependencies += "com.github.axel22" %% "scalameter" % "0.4"

    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")


## Source code

ScalaMeter source code is available at [GitHub](https://github.com/axel22/scalameter).



