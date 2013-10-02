

name := "scalameter"

organization := "com.github.axel22"

version := "0.4-SNAPSHOT"

scalaVersion := "2.10.0"

scalacOptions ++= Seq("-deprecation", "-unchecked")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= List(
  "org.scalatest" % "scalatest_2.10" % "1.9.1",
  "jfree" % "jfreechart" % "1.0.12",
  "org.apache.commons" % "commons-math3" % "3.0",
  "org.scala-tools.testing" % "test-interface" % "0.5"
)





publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>http://axel22.github.com/scalameter/</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://opensource.org/licenses/BSD-3-Clause</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:axel22/scalameter.git</url>
    <connection>scm:git:git@github.com:axel22/scalameter.git</connection>
  </scm>
  <developers>
    <developer>
      <id>axel22</id>
      <name>Aleksandar Prokopec</name>
      <url>http://axel22.github.com/</url>
    </developer>
  </developers>)


