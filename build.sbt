name := "Yamlson"

organization := "com.faacets"

version := "0.1.0-SNAPSHOT"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.7",
  "org.yaml" % "snakeyaml" % "1.14",
  "com.jsuereth" %% "scala-arm" % "1.4"
)

scalaVersion := "2.11.4"

scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation") 
