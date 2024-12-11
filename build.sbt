ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.2"

lazy val root = (project in file("."))
  .settings(
    name := "hello-panama",
    run / fork := true,
    run / javaOptions += "--enable-native-access=ALL-UNNAMED"
  )
