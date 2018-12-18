# episb-bed-json-converter

Re: Progress report

commands to run code:

For loading the LOLACore database:

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

If elasticsearch is ued, all annotations found will be loaded into a local Elasticsearch index named "annotations".

For loading non-headered segmentations, run the following command:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessSegmentationNonHeadered --segname=<name of segmentation> --expname=<name of experiment> --path=<path to bed file> --writer=[elastic|<path to write into]"
``

for example:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ProcessSegmentationNonHeadered --segname=EncodeBroadHmm --expname="my experiment" --path=/home/ognen/wgEncodeBroadHmmGm12878HMM.bed --writer=elastic"
``

The process of loading annotations has now been separated from loading the segmentations. All segmentations found will be loaded into a local Elasticsearch index called "segmentations". See the explanation for the --writer parameter in the section explaining loading of raw LOLA data.
