library(GenomicRanges)
library(GenomicDistributions)
library(rtracklayer)
library(ggplot2)

## the R script below depends on the query GRanges object
## we operate on bed files so we need a way to get a bed file
## into a GRanges object

## first we need to extract bed filename
args <- commandArgs(trailingOnly = TRUE)
bedfname <- args[1]

## and then we read it into GRanges object
gr_obj <- import(bedfname)
query <- split(gr_obj, gr_obj$name)

### now we can proceed with GenomicDistributions extraction
TSSdist = calcFeatureDistRefTSS(query, "hg38")
g = plotFeatureDist(TSSdist, featureName="TSS")
ggsave("bedname_tssdist.png", g)

x = calcChromBinsRef(query, "hg38")
g = plotChromBins(x)
ggsave("bedname_chrombins.png", g)

gcvec = calcGCContentRef(query, "hg38")
g = plotGCContent(gcvec)
ggsave("bedname_gccontent.png", g)

gp = calcPartitionsRef(query, "hg38")
gp$Perc = gp$Freq/length(v)
g = plotPartitions(gp)
ggsave("bedname_partitions.png", g)

bedmeta = list(id="identifier, blah",
	gc_content=mean(gcvec),
	num_regions=length(query),
	mean_abs_tss_dist=mean(abs(TSSdist)),
	genomic_partitions=gp)

l = list("bedname"=bedmeta)

write(jsonlite::toJSON(l, pretty=TRUE), "bedname.json")

