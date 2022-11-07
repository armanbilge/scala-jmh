ThisBuild / tlBaseVersion := "0.0"

ThisBuild / organization := "com.armanbilge"
ThisBuild / organizationName := "Arman Bilge"
ThisBuild / developers += tlGitHubDev("armanbilge", "Arman Bilge")
ThisBuild / startYear := Some(2022)

ThisBuild / crossScalaVersions := Seq("3.2.1")

lazy val root = tlCrossRootProject.aggregate(compilerPlugin, benchmarks)

lazy val compilerPlugin = project
  .in(file("compiler-plugin"))
  .settings(
    name := "scala-jmh-plugin",
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
    ),
  )

lazy val benchmarks = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("benchmarks"))
  .enablePlugins(NoPublishPlugin)
