
name := "SocialNetwork"
scalaVersion := "2.12.7"

version := "1.0"


libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.6.0"

val circeVersion = "0.11.1"
libraryDependencies += "io.circe" %% "circe-core" % "0.11.1"
libraryDependencies += "io.circe" %% "circe-generic" % "0.11.1"
libraryDependencies += "io.circe" %% "circe-parser" % "0.11.1"
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)
