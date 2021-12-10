import ScalaMeterBuild._

lazy val scalaMeterCore = project
  .in(file("scalameter-core"))
  .settings(name := "scalameter-core")
  .settings(scalaMeterCoreSettings)

lazy val scalaMeter = project
  .in(file("."))
  .settings(name := "scalameter")
  .settings(scalaMeterSettings ++ Seq(javaCommandSetting, runsuiteTask)) dependsOn (
  scalaMeterCore
  ) aggregate (
  scalaMeterCore
  )

