# episb-bed-json-converter

Re: Progress report

commands to run code:

For loading the LOLACore database:

``
sbt "runMain com.github.oddodaoddo.sheffieldapp.ElasticLoadRawData <regions/ directory from LOLA core db or a directory complying to such a format>"
``

for example:

``
sbt "runMain com.github.oddodaoddo.sheffieldapp.ElasticLoadRawData /mnt/shefflab/LOLAweb/databases/LOLACore/hg38/encode_tfsb"
``

All annotations found will be loaded into a local Elasticsearch index named "annotations".

For loading non-headered segmentations, run the following command:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ElasticLoadSegmentationNonHeadered --path=<path to bed file> --segname=<desired name of segmentation>"
``

or

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ElasticLoadSegmentationNonHeadered --path=<path to bed file> --segname=<desired name of segmentation> --columns=<comma separated list of numbers (zero indexed)>"
``

for example:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ElasticLoadSegmentationNonHeadered --path=/home/ognen/wgEncodeBroadHmmGm12878HMM.bed --segname=wgEncodeBroadHmmGm12878HMM"
``

All segmentations found will be loaded into a local Elasticsearch index called "segmentations".
