# episb-rest-server #

## Purpose ##

The purpose of the episb-rest-server is to serve the API for the episb server. The code is written in Scala, using the Scalatra framework. In (this) testing/development phase, the server runs on port 8080 and mus be run from the command line like so:

## Build & Run ##

```sh
$ cd episb-rest-server
$ sbt
> jetty:start
```

Then launch a browser and point it to localhost:8080

NB: Going to url:8080/"type anything here" as in the above "localhost:8080/list" will make the server list all the "paths" it is listening to (effectively listing all the REST API points it knows).

## Dependencies ##

The REST API code depends on a running local instance of Elasticsearch. This is where all the queries are performed in the background and results served back to the client.

## Status ##

There are a lot of "FIXME" and configuration issues to get the project to production quality stage. Bear with me :-)

## Testing scenario ##

In order to test the episb-rest-server code, one can add the following line to the build.sbt file in the episb-rest-server directory:

containerPort in Jetty := 8090

Then you can run the server as usual by starting sbt and then typing jetty:start. The server will now run on port 8090 and thus will not interfere with anything that might be running in production on port 8080.

## Future ##

The proper way to run the code is to build it into a .war file (which is built every time a "package") command is run from sbt. The .war artifact can then be copied into a directory of a running Tomcat server, which will in turn "explode" the .war file and serve it immediately.

See this Scalatra doc for production deployment: http://scalatra.org//guides/2.6/deployment/servlet-container.html
