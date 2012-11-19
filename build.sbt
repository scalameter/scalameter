

name := "scalameter"

organization := "org.scalameter"

version := "0.1"

scalaVersion := "2.10.0-RC2"

libraryDependencies ++= List(
  "org.scalatest" % "scalatest_2.10.0-RC2" % "1.8-B2",
  "jfree" % "jfreechart" % "1.0.12",
  "org.apache.commons" % "commons-math3" % "3.0",
  "org.scala-tools.testing" % "test-interface" % "0.5"
)
