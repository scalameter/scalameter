import java.io.File

import sbt.Keys._
import sbt._
import xerial.sbt.Sonatype.SonatypeKeys._

object ScalaMeterBuild {

  val scalaMeterSettings = Seq(
    name := "scalameter",
    organization := "io.github.hughsimpson",
    scalaVersion := "2.13.6",
    crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.6", "3.0.0"),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-Xfuture"),
    libraryDependencies ++= dependencies(scalaVersion.value),
    parallelExecution in Test := false,
    fork := true,
    fork in run := true,
    fork in Test := true,
    outputStrategy := Some(StdoutOutput),
    connectInput in run := true,
    connectInput in Test := true,
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
    ),
    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <url>https://github.com/hughsimpson/scalameter/</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://opensource.org/licenses/BSD-3-Clause</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:hughsimpson/scalameter.git</url>
        <connection>scm:git:git@github.com:hughsimpson/scalameter.git</connection>
      </scm>
      <developers>
        <developer>
          <id>hughsimpson</id>
          <name>Hugh Simpson</name>
          <url>http://github.com/hughsimpson</url>
        </developer>
      </developers>
  )

  def dependencies(scalaVersion: String) = {
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((3, _)) => List(
        "org.scalatest" %% "scalatest" % "3.2.9" % "test",
        "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.apache.commons" % "commons-math3" % "3.2",
        "org.scala-sbt" % "test-interface" % "1.0",
        "org.scala-lang.modules" %% "scala-xml" % "2.0.0",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "2.0.0",
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.0",
        "commons-io" % "commons-io" % "2.4",
        "org.jline" % "jline" % "3.10.0"
      )
      case Some((2, 13)) => List(
        "org.scalatest" %% "scalatest" % "3.2.9" % "test",
        "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.apache.commons" % "commons-math3" % "3.2",
        "org.scala-sbt" % "test-interface" % "1.0",
        "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.0",
        "commons-io" % "commons-io" % "2.4",
        "io.spray" %% "spray-json" % "1.3.5",
        "org.jline" % "jline" % "3.10.0"
      )
      case Some((2, 12)) => List(
        "org.scalatest" % "scalatest_2.12" % "3.2.9" % "test",
        "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.apache.commons" % "commons-math3" % "3.2",
        "org.scala-sbt" % "test-interface" % "1.0",
        "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.5",
        "org.scala-lang.modules" % "scala-parser-combinators_2.12" % "1.0.4",
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.0",
        "commons-io" % "commons-io" % "2.4",
        "io.spray" % "spray-json_2.12" % "1.3.2",
        "org.jline" % "jline" % "3.10.0"
      )
      case Some((2, 11)) => List(
        "org.scalatest" %% "scalatest" % "3.2.9" % "test",
        "org.scalatestplus" %% "scalacheck-1-15" % "3.2.3.0" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.apache.commons" % "commons-math3" % "3.2",
        "org.scala-sbt" % "test-interface" % "1.0",
        "org.scala-lang.modules" %% "scala-xml" % "1.0.1",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1",
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.0",
        "commons-io" % "commons-io" % "2.4",
        "io.spray" %% "spray-json" % "1.3.2",
        "org.jline" % "jline" % "3.10.0"
      )
      case _ => Nil
    }
  }

  val scalaMeterCoreSettings = Seq(
    name := "scalameter-core",
    organization := "io.github.hughsimpson",
    scalaVersion := "2.13.0",
    crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0", "3.0.0"),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-Xfuture", "-language:implicitConversions"),
    libraryDependencies ++= coreDependencies(scalaVersion.value),
    parallelExecution in Test := false,
    fork := true,
    fork in run := true,
    fork in Test := true,
    outputStrategy := Some(StdoutOutput),
    connectInput in run := true,
    connectInput in Test := true,
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
    ),
    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <url>https://github.com/hughsimpson/scalameter/</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://opensource.org/licenses/BSD-3-Clause</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:hughsimpson/scalameter.git</url>
        <connection>scm:git:git@github.com:hughsimpson/scalameter.git</connection>
      </scm>
      <developers>
        <developer>
          <id>hughsimpson</id>
          <name>Hugh Simpson</name>
          <url>http://github.com/hughsimpson</url>
        </developer>
      </developers>
  )

  def coreDependencies(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
    case Some((3, _)) => List(
      "io.github.classgraph" % "classgraph" % "4.8.78",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4",
      "org.scalacheck" %% "scalacheck" % "1.15.4" % "test",
      "org.scalatest" %% "scalatest" % "3.2.9" % "test",
      "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test",
      "junit" % "junit" % "4.12" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.apache.commons" % "commons-math3" % "3.2",
      "org.apache.commons" % "commons-lang3" % "3.4",
      "org.scala-lang.modules" %% "scala-xml" % "2.0.0",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.0.0",
      "org.ow2.asm" % "asm" % "5.0.4",
      "org.jline" % "jline" % "3.10.0"
    )
    case Some((2, 13)) => List(
      "io.github.classgraph" % "classgraph" % "4.8.78",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.1",
      "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
      "org.scalatest" %% "scalatest" % "3.2.9" % "test",
      "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test",
      "junit" % "junit" % "4.12" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.apache.commons" % "commons-math3" % "3.2",
      "org.apache.commons" % "commons-lang3" % "3.4",
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
      "org.ow2.asm" % "asm" % "5.0.4",
      "org.jline" % "jline" % "3.10.0"
    )
    case Some((2, 12)) => List(
      "io.github.classgraph" % "classgraph" % "4.8.78",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.1",
      "org.scalacheck" % "scalacheck_2.12" % "1.13.4" % "test",
      "org.scalatest" % "scalatest_2.12" % "3.2.9" % "test",
      "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test",
      "junit" % "junit" % "4.12" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.apache.commons" % "commons-math3" % "3.2",
      "org.apache.commons" % "commons-lang3" % "3.4",
      "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.5",
      "org.scala-lang.modules" % "scala-parser-combinators_2.12" % "1.0.4",
      "org.ow2.asm" % "asm" % "5.0.4",
      "org.jline" % "jline" % "3.10.0"
    )
    case Some((2, 11)) => List(
      "io.github.classgraph" % "classgraph" % "4.8.78",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.1",
      "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
      "org.scalatest" %% "scalatest" % "3.2.9" % "test",
      "org.scalatestplus" %% "scalacheck-1-15" % "3.2.3.0" % "test",
      "junit" % "junit" % "4.12" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.apache.commons" % "commons-math3" % "3.2",
      "org.apache.commons" % "commons-lang3" % "3.4",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.1",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1",
      "org.ow2.asm" % "asm" % "5.0.4",
      "org.jline" % "jline" % "3.10.0"
    )
    case _ => Nil
  }

  val javaCommand = TaskKey[String](
    "java-command",
    "Creates a java vm command for launching a process."
  )

  val javaCommandSetting = javaCommand := {
    val dp = (dependencyClasspath in Compile).value
    val jar = (artifactPath in(Compile, packageBin)).value
    val testjar = (artifactPath in(Test, packageBin)).value
    val pbc = (packageBin in Compile).value
    val pbt = (packageBin in Test).value
    val cp = dp.map(_.data) :+ jar :+ testjar
    val javacommand = "java -Xmx2048m -Xms2048m -XX:+UseCondCardMark -verbose:gc -server -cp %s".format(
      cp.mkString(File.pathSeparator)
    )
    javacommand
  }

  val runsuiteTask = InputKey[Unit](
    "runsuite",
    "Runs the benchmarking suite."
  ) := Def.inputTask {
    val args = complete.DefaultParsers.spaceDelimited("<arg>").parsed
    val jc = javaCommand.value
    val javacommand = jc
    val comm = javacommand + " " + "org.scalameter.Main" + " " + args.mkString(" ")
    streams.map(_.log.info("Executing: " + comm))
    import sys.process._
    comm !
  }

  /* projects */

}
