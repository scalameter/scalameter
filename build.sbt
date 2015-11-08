import java.io.File
import com.typesafe.sbt.pgp.PgpKeys._
import org.stormenroute.mecha._

import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._

import ReleaseExtras._
import ReleaseExtras.ReleaseExtrasKeys._

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
    publish <<= streams.map(_.log.info("Publishing to Sonatype is disabled since the \"" + publishUser + "\" and/or \"" + publishPass + "\" environment variables are not set."))
})

val releasePluginSettings = releaseSettings ++ Seq(
  releaseBranchName := s"version/${(version in ThisBuild).value}",
  examples.repo := "git@github.com:scalameter/scalameter-examples.git",
  examples.tag := "v%s",
  examples.tagComment := "Release %s",
  examples.commitMessage := "Set ScalaMeter version to %s",
  examples.scalaMeterVersionFile := "version.sbt",
  examples.scalaMeterVersionFileContent := globalVersionString,
  commitMessage := s"Set version to ${(version in ThisBuild).value}",
  publishArtifactsAction <<= publishSigned.map(identity),
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

val scalaMeterSettings = publishCreds ++ Seq(
  crossScalaVersions := Seq("2.10.5", "2.11.7", "2.12.0-M1"),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint"),
  libraryDependencies <++= (scalaVersion)(sv => dependencies(sv)),
  parallelExecution in Test := false,
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
  ),
  publishMavenStyle := true,
  publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
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

def dependencies(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, 12)) => List(
    "org.scalatest" % "scalatest_2.12.0-M1" % "2.2.5-M1" % "test",
    "com.github.wookietreiber" %% "scala-chart" % "0.5.0",
    "org.apache.commons" % "commons-math3" % "3.2",
    "org.scala-tools.testing" % "test-interface" % "0.5",
    "org.scala-lang" % "scala-reflect" % "2.11.7",
    "org.scala-lang.modules" % "scala-xml_2.12.0-M1" % "1.0.5",
    "org.scala-lang.modules" % "scala-parser-combinators_2.12.0-M1" % "1.0.4",
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.5.2"
  )
  case Some((2, 11)) => List(
    "org.scalatest" %% "scalatest" % "2.1.3" % "test",
    "com.github.wookietreiber" %% "scala-chart" % "0.5.0",
    "org.apache.commons" % "commons-math3" % "3.2",
    "org.scala-tools.testing" % "test-interface" % "0.5",
    "org.scala-lang" % "scala-reflect" % "2.11.7",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.1",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.2"
  )
  case Some((2, 10)) => List(
    "org.scalatest" %% "scalatest" % "2.1.2" % "test",
    "com.github.wookietreiber" %% "scala-chart" % "0.5.0",
    "org.apache.commons" % "commons-math3" % "3.2",
    "org.scala-tools.testing" % "test-interface" % "0.5",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.2"
  )
  case _ => Nil
}

val scalaMeterCoreSettings = publishCreds ++ Seq(
  crossScalaVersions := Seq("2.10.5", "2.11.7", "2.12.0-M1"),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint"),
  libraryDependencies <++= (scalaVersion)(sv => coreJavaLibs ++ coreDependencies(sv)),
  parallelExecution in Test := false,
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
  ),
  publishMavenStyle := true,
  publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
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

def coreJavaLibs = Seq(
    "org.apache.commons" % "commons-math3" % "3.2",
    "org.apache.commons" % "commons-lang3" % "3.4",
    "org.ow2.asm" % "asm" % "5.0.4"
  )

def coreDependencies(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, 12)) => Seq(
    "org.scalacheck" % "scalacheck_2.12.0-M1" % "1.12.4" % "test",
    "org.scalatest" % "scalatest_2.12.0-M1" % "2.2.5-M1" % "test",
    "org.scala-lang" % "scala-reflect" % "2.11.7",
    "org.scala-lang.modules" % "scala-xml_2.12.0-M1" % "1.0.5",
    "org.scala-lang.modules" % "scala-parser-combinators_2.12.0-M1" % "1.0.4"
  )
  case Some((2, 11)) => Seq(
    "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
    "org.scalatest" %% "scalatest" % "2.1.3" % "test",
    "org.scala-lang" % "scala-reflect" % "2.11.7",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.1",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
  )
  case Some((2, 10)) => Seq(
    "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
    "org.scalatest" %% "scalatest" % "2.1.3" % "test"
  )
  case _ => Seq()
}

val javaCommand = TaskKey[String](
  "java-command",
  "Creates a java vm command for launching a process."
)

val javaCommandSetting = javaCommand <<= (
  dependencyClasspath in Compile,
  artifactPath in (Compile, packageBin),
  artifactPath in (Test, packageBin),
  packageBin in Compile,
  packageBin in Test
) map {
  (dp, jar, testjar, pbc, pbt) => // -XX:+UseConcMarkSweepGC  -XX:-DoEscapeAnalysis -XX:MaxTenuringThreshold=12 -XX:+PrintGCDetails 
  //val cp = dp.map("\"" + _.data + "\"") :+ ("\"" + jar +"\"") :+ ("\"" + testjar + "\"")
  val cp = dp.map(_.data) :+ jar :+ testjar
  val javacommand = "java -Xmx2048m -Xms2048m -XX:+UseCondCardMark -verbose:gc -server -cp %s".format(
    cp.mkString(File.pathSeparator)
  )
  javacommand
}

val runsuiteTask = InputKey[Unit](
  "runsuite",
  "Runs the benchmarking suite."
) <<= inputTask {
  (argTask: TaskKey[Seq[String]]) =>
  (argTask, javaCommand) map {
    (args, jc) =>
    val javacommand = jc
    val comm = javacommand + " " + "org.scalameter.Main" + " " + args.mkString(" ")
    streams.map(_.log.info("Executing: " + comm))
    import sys.process._
    comm !
  }
}

//projects

lazy val scalaMeterCore = (crossProject in file("scalameter-core")).settings(
  name := "scalameter-core",
  organization := "com.storm-enroute",
  scalaVersion := "2.11.7"
).settings(
  releasePluginSettings : _*
).jvmSettings(
  scalaMeterCoreSettings : _*
).jsSettings(
  jsDependencies += "org.webjars.bower" % "jstat" % "1.3.0" / "jstat.js" minified "jstat.min.js"
)

lazy val scalaMeterCoreJVM = scalaMeterCore.jvm
lazy val scalaMeterCoreJS = scalaMeterCore.js

lazy val scalaMeter = (crossProject in file(".")).settings(
  name := "scalameter",
  organization := "com.storm-enroute",
  scalaVersion := "2.11.7"
).settings(
  releasePluginSettings : _*
).jvmSettings(
  scalaMeterSettings ++ Seq(javaCommandSetting, runsuiteTask) : _*
).jsSettings(
  libraryDependencies += "org.scala-js" %%% "scala-parser-combinators" % "1.0.2"
) dependsOn (
  scalaMeterCore
) aggregate(
  scalaMeterCore
)

lazy val scalaMeterJVM = scalaMeter.jvm
lazy val scalaMeterJs = scalaMeter.js
