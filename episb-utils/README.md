# episb-bed-json-converter

*NB: The code in this repo is transitioning away from manipulating Elasticsearch databases for the EPISB project by itself. Instead, we are using the REST API points provided by the episb-rest-server project.*

*NB2: There are two ways to run things - one is on a personal (dev) machine, the other is in production (for now a single DC/OS machine at UVA). I shall explain both below:*

Things necessary to run our code on a personal machine:
<ul>
<li>Elasticsearch</li>
<li>Kibana (optional but helps to check database, run ad-hoc queries, etc.)</li>
</ul>

Install Elasticsearch by whatever means your OS supports or download it [here](https://www.elastic.co/downloads/past-releases/elasticsearch-6-4-2). We use version 6.4.2 right now. Do the same for [Kibana](https://www.elastic.co/downloads/past-releases/kibana-6-4-2)

The only thing you will need for Elastic to run out of the box with our code right now is this elasticsearch.yml file below. Create it using whatever editor you prefer and use it to replace the elasticsearch.yml that came with the default install. 

```
cluster.name: episb-elastic-cluster
path.data: /var/db/elasticsearch
path.logs: /var/log/elasticsearch
xpack.ml.enabled: false
```

The path.data and path.logs paths may be different on your local machine, depending on the way elasticsearch was installed.

Then start elasticsearch and kibana (in sequence one after the other). After a few minutes, you should be able to visit http://localhost:5601 to launch a Kibana console.

If you have the proper credentials to ssh into the DC/OS machine we have running our experimental setup at UVA, you can tunnel to Kibana such as:

```
ssh -4 -fN -L 5601:localhost:5601 <username>@episb.org
```

Now you can open the browser and visit http://localhost:5601 like you would if Kibana was running on your own machine.

For all the following commands, there are variable assumptions outlined below:

* You are using ssh to log into episb.org and run these commands from a local clone of the [git repo](https://github.com/databio/episb-provider). This means that the [API server](https://github.com/databio/episb-provider/tree/master/episb-rest-server) is already set up for you and is running.
* In this case the various databases such as LOLA are mounted using NFS on the machine.

or,

* You are running this on your own machine.
* This means Elasticsearch is set up and running and
* You are running the episb-rest-server by yourself (see separate instructions on how to do this [here](https://github.com/databio/episb-provider/tree/master/episb-rest-server) and
* You have Java installed on the machine, as well as [sbt](https://www.scala-sbt.org/)
* Side note: there is a way to run all the Scala code without sbt and just by using Java - I will provide these instructions later.

Below examples assume you have done a

```
git clone git@github.com:databio/episb-provider.git
cd episb-provider/episb-utils
```

Without further ado:

## For loading the LOLACore database (DEPRECATED while we re-assess how to load LOLA Core with the new annotation/segmentation pairing format!)

``
sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessRawLOLAData --writer=[elastic|<path to write into>] <regions/ directory from LOLA core db or a directory complying to such a format>"
``

for example:

``
sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessRawLOLAData --writer=elastic /mnt/shefflab/LOLAweb/databases/LOLACore/hg38/encode_tfsb"
``

-> will use the elasticsearch settings in src/main/resources/application.conf to commit all data to elastic.

``
sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessRawLOLAData --writer=output.json /mnt/shefflab/LOLAweb/databases/LOLACore/hg38/encode_tfsb"
``

-> will commit all output to a local json file called output.json. Elastic is not used.

If elasticsearch is used, all annotations found will be loaded into a local Elasticsearch index named "annotations".

## For loading non-headered segmentations, run the following command:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessSegmentationNonHeadered --segname=<name of segmentation> --expname=<name of experiment> --path=<path to bed file> --writer=[elastic|<path to write into]"
``

for example:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessSegmentationNonHeadered --segname=EncodeBroadHmm --expname="my_experiment_name" --path=/home/ognen/wgEncodeBroadHmmGm12878HMM.bed --writer=elastic"
``

The process of loading annotations has now been separated from loading the segmentations. All segmentations found will be loaded into a local Elasticsearch index called "segmentations". See the explanation for the --writer parameter in the section explaining loading of raw LOLA data above.

## For loading non-headered annotations, run the following command:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessAnnotationNonHeadered --segname=encodebroadhmm --expname="my_experiment_name" --readfrom=/home/ognen/wgEncodeBroadHmmGm12878HMM.bed --writeto=/tmp/output.txt --column=<column to get annotation value from>"
``

The above code will:

<ul>
<li>Read a bed file from the --readfrom parameter</li>
<li>Use the --segname parameter to load into memory a segmentation from the episb-rest-server, if one exists</li>
<li>Read above bed file line by line, pick out the annotation from --column, pick out segChr, segStart and segEnd values from each line</li>
<li>Use above segChr, segStart and segEnd to search for exact match in segmentation</li>
<li>If exact match is found, produce tuple (segname:segmentID) and commit such tuple to --writeto parameter file</li>
<li>Use such file to call an API point at episb-rest-server to load in the annotation into elastic</li>
<li>Produce a design interface JSON document for said experiment/segmentation coupling</li>
<li>Use episb-rest-server to load in the design interface into elastic back-end</li>
</ul>

This command assumes:

1. You know what column to use for an annotation value
2. The episb-rest-server is running with an elasticsearch backend behind it
3. All names of segmentations are lowercase (for now). This is because by default elastic does not recognize things with capital letters when searching.
4. A segmentation exists in elastic by the name you are using in the --segname parameter.
