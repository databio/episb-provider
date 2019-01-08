# episb-bed-json-converter

commands to run code:

# For loading the LOLACore database:

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

# For loading non-headered segmentations, run the following command:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessSegmentationNonHeadered --segname=<name of segmentation> --expname=<name of experiment> --path=<path to bed file> --writer=[elastic|<path to write into]"
``

for example:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessSegmentationNonHeadered --segname=EncodeBroadHmm --expname="my_experiment_name" --path=/home/ognen/wgEncodeBroadHmmGm12878HMM.bed --writer=elastic"
``

The process of loading annotations has now been separated from loading the segmentations. All segmentations found will be loaded into a local Elasticsearch index called "segmentations". See the explanation for the --writer parameter in the section explaining loading of raw LOLA data above.

# For loading non-headered annotations, run the following command:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessAnnotationNonHeadered --segname=EncodeBroadHmm --expname="my_experiment_name" --readfrom=/home/ognen/wgEncodeBroadHmmGm12878HMM.bed --writeto=<file name such as /tmp/output.txt> --column=<column to get annotation value from>"
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
