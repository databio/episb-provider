organization := "com.oddodaoddo.github"

name := "episb-utils"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.8"

ensimeIgnoreScalaMismatch in ThisBuild := true

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "com.github.jsonld-java" % "jsonld-java" % "0.12.1",
  "org.json4s" %% "json4s-native" % "3.6.1",
  "com.amazonaws" % "aws-java-sdk" % "1.11.463",
  "com.typesafe" % "config" % "1.3.3",
  "org.elasticsearch" % "elasticsearch" % "6.4.2",
  "org.elasticsearch.client" % "transport" % "6.4.2",
  "org.elasticsearch.client" % "elasticsearch-rest-client" % "6.4.2",
  "org.rogach" %% "scallop" % "3.1.5",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalaj" %% "scalaj-http" % "2.4.1"
)
