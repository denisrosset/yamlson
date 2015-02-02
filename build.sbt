name := "Yamlson"

organization := "com.faacets"

version := "0.1.1"

licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php"))

homepage := Some(url("https://github.com/denisrosset/consolidate"))

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.7",
  "org.yaml" % "snakeyaml" % "1.14",
  "com.jsuereth" %% "scala-arm" % "1.4"
)

scalaVersion := "2.11.5"

scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation") 

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}


pomExtra := (
  <scm>
    <url>git@github.com:denisrosset/yamlson.git</url>
    <connection>scm:git:git@github.com:denisrosset/yamlson.git</connection>
  </scm>
  <developers>
    <developer>
      <id>denisrosset</id>
      <name>Denis Rosset</name>
      <url>http://denisrosset.com</url>
    </developer>
  </developers>
)
