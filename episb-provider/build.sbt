val ScalatraVersion = "2.6.5"

organization := "com.github.oddodaoddo"

name := "episb-provider"

version := "0.4.0"

scalaVersion := "2.12.6"

//ensimeIgnoreScalaMismatch in ThisBuild := true

lazy val core = RootProject(file("../episb-utils"))

val main = Project(id = "application", base = file(".")).dependsOn(core)

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
	"org.scalatra" %% "scalatra-swagger"  % ScalatraVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.9.v20180320" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "com.github.jsonld-java" % "jsonld-java" % "0.12.1",
  "org.json4s" %% "json4s-native" % "3.6.1",
  "com.typesafe" % "config" % "1.3.3",
  "com.typesafe.slick" %% "slick" % "3.3.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.0",
  "org.postgresql" % "postgresql" % "42.2.4",
  "org.elasticsearch" % "elasticsearch" % "6.4.2",
  "org.elasticsearch.client" % "transport" % "6.4.2",
  "org.elasticsearch.client" % "elasticsearch-rest-client" % "6.4.2"
)

// uncomment following line to bind to a different port (default is 8080)
// containerPort in Jetty := 8090

enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)
