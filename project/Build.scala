import sbt._
import Keys._
import Process._
import java.io.File



object ScalaMeterBuild extends Build {

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

  val scalaMeterSettings = Defaults.defaultSettings ++ publishCreds ++ Seq(
    scalaVersion := "2.10.2",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint"),
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
    ),
    libraryDependencies ++= List(
      "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
      "jfree" % "jfreechart" % "1.0.12",
      "org.apache.commons" % "commons-math3" % "3.0",
      "org.scala-tools.testing" % "test-interface" % "0.5"
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
        <url>git@github.com:axel22/scalameter.git</url>
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
  
  /* projects */
  
  lazy val scalameter = Project(
    "scalameter",
    file("."),
    settings = scalaMeterSettings
  ) dependsOn (
  )
  
}
