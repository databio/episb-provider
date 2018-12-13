organization := "com.oddodaoddo.github"

name := "episb-bed-JsonLD-converter"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.4"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "com.github.jsonld-java" % "jsonld-java" % "0.12.1",
	"org.json4s" %% "json4s-native" % "3.6.1",
  "com.amazonaws" % "aws-java-sdk" % "1.11.463",
  "com.typesafe" % "config" % "1.3.3",
  "org.elasticsearch" % "elasticsearch" % "6.4.2",
  "org.elasticsearch.client" % "transport" % "6.4.2",
  "org.elasticsearch.client" % "elasticsearch-rest-client" % "6.4.2"

)
