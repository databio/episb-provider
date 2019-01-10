val ScalatraVersion = "2.6.4"

organization := "com.github.oddodaoddo"

name := "episb-provider"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.8"

ensimeIgnoreScalaMismatch in ThisBuild := true

lazy val core = RootProject(file("../episb-utils"))

val main = Project(id = "application", base = file(".")).dependsOn(core)

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
	"org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.9.v20180320" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "com.github.jsonld-java" % "jsonld-java" % "0.12.1",
  "org.json4s" %% "json4s-native" % "3.6.1",
  "com.amazonaws" % "aws-java-sdk" % "1.11.429",
  "com.typesafe" % "config" % "1.3.3",
  "org.elasticsearch" % "elasticsearch" % "6.4.2",
  "org.elasticsearch.client" % "transport" % "6.4.2",
  "org.elasticsearch.client" % "elasticsearch-rest-client" % "6.4.2"
)

enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)
