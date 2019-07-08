library(GenomicDistributions)

# set query to bed file

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

