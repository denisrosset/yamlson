name := "yamlson"

organization := "com.faacets"

scalaVersion := "2.11.7"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

homepage := Some(url(s"https://github.com/denisrosset/${name.value}#readme"))

scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-optimize") 

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.10",
  "org.yaml" % "snakeyaml" % "1.16",
  "com.jsuereth" %% "scala-arm" % "1.4"
)

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.jcenterRepo
)

publishArtifact in Test := false


// This will break the process into two parts:
// First, stage all artifacts using publish.
// Once all artifacts are staged, run bintrayRelease to make the artifacts public

bintrayReleaseOnPublish in ThisBuild := false

bintrayRepository := "com.faacets"
