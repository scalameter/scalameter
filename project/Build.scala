
import sbt._
import Keys._
import Process._
import java.io.File



object ScalaMeterBuild extends Build {
  
  /* tasks and settings */
  
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
      println("Executing: " + comm)
      val exitcode = comm!;
      if (exitcode != 0) error("Tests not passing.")
    }
  }
  
  /* projects */
  
  lazy val scalameter = Project(
    "scalameter",
    file("."),
    settings = Defaults.defaultSettings ++ Seq(runsuiteTask, javaCommandSetting)
  ) dependsOn (
  )
  
}
