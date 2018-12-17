# episb-bed-json-converter

Re: Progress report

commands to run code:

For loading the LOLACore database:

``
sbt "runMain com.github.oddodaoddo.sheffieldapp.ElasticLoadRawData <regions/ directory from LOLA core db or a directory complying to such a format>"
``

for example:

``
sbt "runMain com.github.oddodaoddo.sheffieldapp.ElasticLoadRawData /mnt/shefflab/LOLAweb/databases/LOLACore/hg38/encode_tfsb
``

For loading non-headered segmentations, run the following command:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ElasticLoadSegmentationNonHeadered --path=<path to bed file> --segname=<desired name of segmentation>"
``

for example:

``
SBT_OPTS="-Xmx16G" sbt "runMain com.github.oddodaoddo.sheffieldapp.ElasticLoadSegmentationNonHeadered --path=/home/ognen/wgEncodeBroadHmmGm12878HMM.bed --segname=wgEncodeBroadHmmGm12878HMM"
``

Code can also be compiled via "sbt compile" and the resulting jar can be run via basic java commands (thus bypassing sbt) - I will include instructions on this later.

The program will create matching .jsonld fiules for each bed file found in index.txt listing as per LOLA database. It will also attempt to load the json documents into elasticsearch server on localhost.

Code is VERY beta - needs a lot more polishing, refactoring, cleaning up and error checking.
