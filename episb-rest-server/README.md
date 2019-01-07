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

## REST API POINTS ##

*/segmentations/get/ByNameWithSegments/:segmentationName?compressed=true/false (GET)*

Returns a segmentation from elastic search (a list of segments with their IDs)

*/experiments/add/preformatted/:experimentName/:segmentationName (POST)*

Takes in a file formatted such as: "annotation_value\<tab\>segmentation_name::segment_id"

Right now we are not verifying much. It is the responsibility of the caller to make sure the segmentation exists.
The file to feed this API point can be produced by running the following from episb-bed-json-converter suite of programs:

cd ../episb-bed-json-converter
SBT_OPTS="-Xmx12G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessAnnotationNonHeadered --segname=name_of_segmentation --expname=name_of_experiment --readfrom=path_to_experiment_bed_file --writeto=path_to_output_file --column=colummn_to_use_for_annotation_value"

the SBT_OPTS above is optional but recommended.

To call the API point, create a following file (e.g. /tmp/multipart-message.data

--a93f5485f279c0
content-disposition: form-data; name="expfile"; filename="exp.out"

Then 'cat path_to_output_file >> /tmp/multipart-message.data'

Then 'echo "--a93f5485f279c0--" >> /tmp/multipart-message.data'

Finally, test the API point by doing the following:

curl http://localhost:8080/experiments/add/preformatted/testexperiment/testsegmentation --data-binary @/tmp/multipart-message.data -X POST -i -H "Content-Type: multipart/form-data; boundary=a93f5485f279c0"

if you have the right segmentation name - it will work.

The way the output file is created is via pulling the entire segmentation into memory and reorganizing it into a hash table indexed by chr (dividing it into chr buckets). The it reads the experiment file into memory and goes through it line by line. For each line it extracts the chr/start/stop components and the annotation value at the "columnt" that was passed to it. It will then search for an EXACT match in the segmentation and if it finds it, it will get the segment ID from the segmentatoon, include the annotation value and that ID into the output file. This is the file you upload to create an experiment.

*/segmentations/update (POST)*

The request body of this API point should contain a design interface describing an experiment served by the server.

## Future ##

The proper way to run the code is to build it into a .war file (which is built every time a "package") command is run from sbt. The .war artifact can then be copied into a directory of a running Tomcat server, which will in turn "explode" the .war file and serve it immediately.

See this Scalatra doc for production deployment: http://scalatra.org//guides/2.6/deployment/servlet-container.html
