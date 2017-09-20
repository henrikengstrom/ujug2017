import sbt.Keys.libraryDependencies

val project = Project(id = "akka-streams-jug", base = file(".")) //enablePlugins (Cinnamon)

name := """akka-streams-jug"""
version := "1.0"
scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.4"
lazy val akkaHttpVersion = "10.0.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion
)

connectInput in run := true
