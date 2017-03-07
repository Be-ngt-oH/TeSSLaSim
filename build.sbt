lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.github.scopt" %% "scopt" % "3.5.0",
    "io.circe" %% "circe-generic" % "0.6.1"
  )
)

lazy val shared = (project in file("shared")).
  settings(commonSettings: _*).
  settings(
    version := "0.1.0"
  )

lazy val scenarioParser = (project in file("scenarioParser")).
  settings(commonSettings: _*).
  settings(
    version := "0.1.0",
    libraryDependencies += "org.parboiled" %% "parboiled" % "2.1.3"
  ).
  dependsOn(shared % "test->test;compile->compile")

lazy val simulatorCore = (project in file("simulatorCore")).
  settings(commonSettings: _*).
  settings(
    version := "0.1.0"
  ).
  dependsOn(scenarioParser, shared % "test->test;compile->compile")

val http4sVersion = "0.15.2"

lazy val webService = (project in file("webService")).
  settings(commonSettings: _*).
  settings(
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.slf4j" % "slf4j-simple" % "1.7.22"
    )
  ).
  dependsOn(simulatorCore, shared)