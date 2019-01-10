# episb-provider #

## Purpose ##

The purpose of the episb-rest-server is to serve the API for the episb server. The code is written in Scala, using the Scalatra framework. In (this) testing/development phase, the server runs on port 8080 and must be run from the command line like so:

## Build & Run ##

```sh
$ cd episb-rest-server
$ sbt
> jetty:start
```

The included script restart_tomcat.sh is provided if you choose to run the provider in a more robust (production) fashion. Instead of using the jetty engine included with the Scalatra framework, it relies on a local install of Tomcat. In particular, the location where the app will be "exploded" (Tomcat terminology that roughly means "unpacked and installed") is assumed to be the /usr/share/tomcat/webapps/ directory. If your installation of tomcat has a different webapps directory please adjust accordingly. The included script will simply package a "war" file from the code and copy this war file under the name /usr/share/tomcat/webapps/episb-provider.war. Tomcat will sense that this file is there, automatically "explode" it and serve whatever Java application is in this file at the http://localhost:8080/episb-provider/ path.

Hence, all APIs in the episb-provider system have the same starting path above.

For more Tomcat configuration, the files usually reside in a location such as /etc/tomcat (but can vary with Linux or BSD flavors). For more informatiom on configuring Tomcat see [this document](https://tomcat.apache.org/tomcat-8.5-doc/index.html).
## Test install ##

After the above steps are completed, launch a browser and point it to localhost:8080/episb-provider/list

*NB: Going to url:8080/"type anything here" as in the above "localhost:8080/episb-provider/list" will make the server list all the "paths" it is listening to (effectively listing all the REST API points it knows).*

## Dependencies ##

The REST API code depends on a running local instance of [Elasticsearch](https://www.elastic.co/products/elasticsearch). This is where all the queries are performed in the background and results served back to the client. Currently we depend on Elasticsearch 6.4.2. Usually Elastic can be installed using whatever facilities the underlying operating system provides (yum for Redhat derivatives, apt for Debian variants, pkg install for BSD variants, so on and so on).

Configuring elasticsearch for optimal use is beyond the scope of this document. We are including a Docker compose file that will allow you to run elasticsearch as a collection of container instances [here](https://github.com/databio/episb-provider/tree/master/episb-provider/elastic). Do note that the docker-compose file will spin off five Docker containers each running elastic with a heap size of 32Gb! They will all run on a single machine as well, meaning that you should plan on this machine having ample memory. If Docker is not applicable to your setup - elasticsearch is happy to run as a basic install as mentioned above. We are including an elasticsearch.yml file to be used for a basic elastic install by way of copying it into /etc/elasticsearch/ (or /usr/local/etc/elasticsearch on FreeBSD, for example).

In order to check what is stored/indexed by Elasticsearch, a developer concole called [Kibana](https://www.elastic.co/products/kibana) is used (however, Elastic supports access via the http protocol so a basic "curl" will do as well!). It is beyond the scope of this document to provide instruction on Elasticsearch query DSL/APIs or Kibana.

## Status ##

There are a lot of "FIXME" and configuration issues to get the project to production quality stage. Bear with us :-)

## Data stored in Elasticsearch ##

Before we discuss the provided REST API points, see this [document](https://github.com/databio/episb-hub/blob/master/docs/data-organization.md) for more information on how data is organized in the episb-provider's Elasticsearch backend.

## REST API POINTS ##

**/segmentations/get/ByNameWithSegments/:segmentationName?compressed=true/false (GET)**

Returns a segmentation from elastic search (a list of segments with their IDs)

**/experiments/add/preformatted/:experimentName/:segmentationName (POST)**

Takes in a file formatted such as: "annotation_value\<tab\>segmentation_name::segment_id"

Right now we are not verifying much. It is the responsibility of the caller to make sure the segmentation exists.

See documentation in [episb-bed-json-provider](https://github.com/databio/episb-provider/tree/master/episb-bed-json-converter) on how to create this file (and the whole process surrounding it). *Nota bene*: the loading of the annotations is automated in the episb-bed-json-converted code, as a side-effect the file is created and can be loaded manually using curl (as below), for testing purposes.

To call the API point, create a following file (e.g. /tmp/multipart-message.data)

--a93f5485f279c0
content-disposition: form-data; name="expfile"; filename="exp.out"

Then 

``
cat path_to_output_file >> /tmp/multipart-message.data
``

Then 

``
echo "--a93f5485f279c0--" >> /tmp/multipart-message.data
``

Finally, test the API point by doing the following:

``
curl http://localhost:8080/experiments/add/preformatted/testexperiment/testsegmentation --data-binary @/tmp/multipart-message.data -X POST -i -H "Content-Type: multipart/form-data; boundary=a93f5485f279c0"
``

If you have the right segmentation name - it will work, no error checking right now.

**/segmentations/update (POST)**

The request body of this API point should contain a design interface describing an experiment served by the server.

## Future ##

The proper way to run the code is to build it into a .war file (which is built every time a "package") command is run from sbt. The .war artifact can then be copied into a directory of a running Tomcat server, which will in turn "explode" the .war file and serve it immediately.

See this Scalatra doc for production deployment: http://scalatra.org//guides/2.6/deployment/servlet-container.html
