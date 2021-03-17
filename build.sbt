import ScalaMeterBuild._

lazy val scalaMeterCore = project
  .in(file("scalameter-core"))
  .settings(name := "scalameter-core")
  .settings(scalaMeterCoreSettings ++ releasePluginSettings).enablePlugins(ReleasePlugin)

lazy val scalaMeter = project
  .in(file("."))
  .settings(name := "scalameter")
  .settings(scalaMeterSettings ++ Seq(javaCommandSetting, runsuiteTask) ++ releasePluginSettings) dependsOn (
  scalaMeterCore
  ) aggregate (
  scalaMeterCore
  ) enablePlugins (
  ReleasePlugin
  )

