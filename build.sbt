name := "Yamlson"

organization := "com.faacets"

version := "0.1.1"

scalaVersion := "2.11.7"

licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php"))

homepage := Some(url("https://github.com/denisrosset/yamlson"))

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

publishTo := Some("Bintray API Realm" at s"https://api.bintray.com/content/denisrosset/com.faacets/uamlson/$version")

credentials += Credentials(new File("credentials.properties"))
