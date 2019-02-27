


import java.io.File
import com.typesafe.sbt.pgp.PgpKeys._
import org.stormenroute.mecha._
import sbt._
import sbt.Keys._
import sbt.Process._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import ReleaseExtras._
import ReleaseExtras.ReleaseExtrasKeys._



object ScalaMeterBuild extends MechaRepoBuild {

  def repoName = "scalameter"

  val publishUser = "SONATYPE_USER"
  val publishPass = "SONATYPE_PASS"

  val userPass = for {
    user <- sys.env.get(publishUser)
    pass <- sys.env.get(publishPass)
  } yield (user, pass)

  val publishCreds: Seq[Setting[_]] = Seq(userPass match {
    case Some((user, pass)) =>
      credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
    case None =>
      // prevent publishing
      publish := streams.value.log.info("Publishing to Sonatype is disabled since the \"" + publishUser + "\" and/or \"" + publishPass + "\" environment variables are not set.")
  })

  val releasePluginSettings = Seq(
    releaseBranchName := s"version/${(version in ThisBuild).value}",
    examples.repo := "git@github.com:scalameter/scalameter-examples.git",
    examples.tag := "v%s",
    examples.tagComment := "Release %s",
    examples.commitMessage := "Set ScalaMeter version to %s",
    examples.scalaMeterVersionFile := "version.sbt",
    examples.scalaMeterVersionFileContent := globalVersionString,
    releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}",
    releasePublishArtifactsAction := publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      branchRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges,
      bumpUpVersionInExamples
    )
  )

  val scalaMeterSettings = MechaRepoPlugin.defaultSettings ++ publishCreds ++ Seq(
    name := "scalameter",
    organization := "com.storm-enroute",
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0-M3"),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-Xfuture"),
    libraryDependencies ++= dependencies(scalaVersion.value),
    parallelExecution in Test := false,
    fork := true,
    fork in run := true,
    fork in Test := true,
    outputStrategy := Some(StdoutOutput),
    connectInput in run := true,
    connectInput in Test := true,
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
    ),
    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <url>http://scalameter.github.io/</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://opensource.org/licenses/BSD-3-Clause</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:scalameter/scalameter.git</url>
        <connection>scm:git:git@github.com:scalameter/scalameter.git</connection>
      </scm>
      <developers>
        <developer>
          <id>axel22</id>
          <name>Aleksandar Prokopec</name>
          <url>http://axel22.github.com/</url>
        </developer>
      </developers>
  )

  def dependencies(scalaVersion: String) = {
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) => List(
        "org.scalatest" % "scalatest_2.13.0-M2" % "3.0.4" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.apache.commons" % "commons-math3" % "3.2",
        "org.scala-tools.testing" % "test-interface" % "0.5",
        "org.scala-lang.modules" % "scala-xml_2.13.0-M2" % "1.0.6",
        "org.scala-lang.modules" % "scala-parser-combinators_2.13.0-M2" % "1.0.7",
        "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.9.8",
        "org.mongodb.scala" % "mongo-scala-driver_2.12" % "2.2.0",
        "commons-io" % "commons-io" % "2.4",
        "io.spray" %  "spray-json_2.13.0-M2" % "1.3.4",
        "org.jline" % "jline" % "3.10.0"
      )
      case Some((2, 12)) => List(
        "org.scalatest" % "scalatest_2.12" % "3.0.0" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.apache.commons" % "commons-math3" % "3.2",
        "org.scala-tools.testing" % "test-interface" % "0.5",
        "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.5",
        "org.scala-lang.modules" % "scala-parser-combinators_2.12" % "1.0.4",
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.8",
        "org.mongodb.scala" %% "mongo-scala-driver" % "2.2.0",
        "commons-io" % "commons-io" % "2.4",
        "io.spray" %  "spray-json_2.12" % "1.3.2",
        "org.jline" % "jline" % "3.10.0"
      )
      case Some((2, 11)) => List(
        "org.scalatest" %% "scalatest" % "3.0.0" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.apache.commons" % "commons-math3" % "3.2",
        "org.scala-tools.testing" % "test-interface" % "0.5",
        "org.scala-lang.modules" %% "scala-xml" % "1.0.1",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1",
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.8",
        "org.mongodb.scala" %% "mongo-scala-driver" % "2.2.0",
        "commons-io" % "commons-io" % "2.4",
        "io.spray" %%  "spray-json" % "1.3.2",
        "org.jline" % "jline" % "3.10.0"
      )
      case _ => Nil
    }
  }

  val scalaMeterCoreSettings = MechaRepoPlugin.defaultSettings ++ publishCreds ++ Seq(
    name := "scalameter-core",
    organization := "com.storm-enroute",
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0-M3"),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-Xfuture"),
    libraryDependencies ++= coreDependencies(scalaVersion.value),
    parallelExecution in Test := false,
    fork := true,
    fork in run := true,
    fork in Test := true,
    outputStrategy := Some(StdoutOutput),
    connectInput in run := true,
    connectInput in Test := true,
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
    ),
    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <url>http://scalameter.github.io/</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://opensource.org/licenses/BSD-3-Clause</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:scalameter/scalameter.git</url>
        <connection>scm:git:git@github.com:scalameter/scalameter.git</connection>
      </scm>
      <developers>
        <developer>
          <id>axel22</id>
          <name>Aleksandar Prokopec</name>
          <url>http://axel22.github.com/</url>
        </developer>
      </developers>
  )

  def coreDependencies(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 13)) => List(
      "org.scalacheck" % "scalacheck_2.13.0-M2" % "1.13.5" % "test",
      "org.scalatest" % "scalatest_2.13.0-M2" % "3.0.4" % "test",
      "junit" % "junit" % "4.12" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.apache.commons" % "commons-math3" % "3.2",
      "org.apache.commons" % "commons-lang3" % "3.4",
      "org.scala-lang.modules" % "scala-xml_2.13.0-M2" % "1.0.6",
      "org.scala-lang.modules" % "scala-parser-combinators_2.13.0-M2" % "1.0.7",
      "org.ow2.asm" % "asm" % "5.0.4",
      "org.jline" % "jline" % "3.10.0"
    )
    case Some((2, 12)) => List(
      "org.scalacheck" % "scalacheck_2.12" % "1.13.4" % "test",
      "org.scalatest" % "scalatest_2.12" % "3.0.0" % "test",
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
      "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test",
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
    val jar = (artifactPath in (Compile, packageBin)).value
    val testjar = (artifactPath in (Test, packageBin)).value
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
  ) := Def.inputTask{
    val args = complete.DefaultParsers.spaceDelimited("<arg>").parsed
    val jc = javaCommand.value
    val javacommand = jc
    val comm = javacommand + " " + "org.scalameter.Main" + " " + args.mkString(" ")
    streams.map(_.log.info("Executing: " + comm))
    import sys.process._
    comm !
  }

  /* projects */

  lazy val scalaMeterCore = Project(
    "scalameter-core",
    file("scalameter-core"),
    settings = scalaMeterCoreSettings ++ releasePluginSettings
  ).enablePlugins(ReleasePlugin)

  lazy val scalaMeter = Project(
    "scalameter",
    file("."),
    settings = scalaMeterSettings ++ Seq(javaCommandSetting, runsuiteTask) ++ releasePluginSettings
  ) dependsOn (
    scalaMeterCore
  ) aggregate(
    scalaMeterCore
  ) enablePlugins(
    ReleasePlugin
  )

}
