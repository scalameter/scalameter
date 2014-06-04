
// Vlad: the plugin interferes with the credentials file
// addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.7")
// to publish, create a file with the following info:
// ```
//    realm=Sonatype Nexus Repository Manager
//    host=oss.sonatype.org
//    user=<user>
//    password=<pass>
// ```
// and use:
// ```
//    sbt -Dscalameter.maven.credentials-file=/path/to/credentials/file
// ```
// This should give you access to the repository.

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.2.0")
