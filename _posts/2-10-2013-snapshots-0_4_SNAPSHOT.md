---
layout: default
title: ScalaMeter Snapshots on Sonatype OSS
poster: Vlad
---


ScalaMeter snapshots are now deployed to Sonatype nightly!

You can add it as an SBT dependency at Sonatype OSS.

    <dependency>
      <groupId>com.github.axel22</groupId>
      <artifactId>scalameter_2.10</artifactId>
      <version>0.4-SNAPSHOT</version>
    </dependency>

Or, in SBT:

    resolvers += "Sonatype OSS Snapshots" at
      "https://oss.sonatype.org/content/repositories/snapshots"

    libraryDependencies += "com.github.axel22" %% "scalameter" % "0.4-SNAPSHOT"


