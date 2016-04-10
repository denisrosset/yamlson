name := "yamlson"

organization := "com.faacets"

scalaVersion := "2.11.8"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

homepage := Some(url(s"https://github.com/denisrosset/${name.value}#readme"))

scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-optimize", "-Ywarn-unused-import")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0-M7" % "test",
  "io.argonaut" %% "argonaut" % "6.2-M1",
  "org.yaml" % "snakeyaml" % "1.17",
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

bintrayRepository := "maven"

scalacOptions ++= commonScalacOptions.diff(Seq("-Ywarn-value-discard"))

warnUnusedImport

lazy val warnUnusedImport = Seq(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) =>
        Seq()
      case Some((2, n)) if n >= 11 =>
        Seq() // TODO Seq("-Ywarn-unused-import")
    }
  },
  scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)},
  scalacOptions in (Test, console) <<= (scalacOptions in (Compile, console))
)

lazy val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yinline-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)
