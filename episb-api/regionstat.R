library(GenomicDistributions)


rdb = LOLA::loadRegionDB("/ext/qumulo/resources/regions/LOLACore/hg38")

rdb$regionGRL
rdb$regionAnno[1,"filename"]


length(rdb$regionGRL)
lapply(seq(1, 3), 
	function(x) {
		doitall(rdb$regionGRL[[x]], rdb$regionAnno[x,]$filename)
	}
	)

# set query to bed file
fileid = "E125-DNase.macs2"
fn = "/ext/qumulo/resources/regions/LOLARoadmap/hg38/roadmap_epigenomics/regions/E125-DNase.macs2.narrowPeak"

outfolder = "test/"  # Set to '' for cwd

#' query = rtracklayer::import(fn)
query = LOLA::readBed(fn)

doitall = function(query, fileid) {

	TSSdist = calcFeatureDistRefTSS(query, "hg38")
	g = plotFeatureDist(TSSdist, featureName="TSS")
	ggplot2::ggsave(paste0(outfolder, fileid, "_tssdist.png"), g)

	x = calcChromBinsRef(query, "hg38")
	g = plotChromBins(x)
	ggplot2::ggsave(paste0(outfolder, fileid, "_chrombins.png"), g)

	gcvec = calcGCContentRef(query, "hg38")
	g = plotGCContent(gcvec)
	ggplot2::ggsave(paste0(outfolder, fileid, "_gccontent.png"), g)

	gp = calcPartitionsRef(query, "hg38")
	gp$Perc = gp$Freq/length(query)
	g = plotPartitions(gp)
	ggplot2::ggsave(paste0(outfolder, fileid, "_partitions.png"), g)

	l = list()
	bedmeta = list(id=fileid,
		gc_content=mean(gcvec),
		num_regions=length(query),
		mean_abs_tss_dist=mean(abs(TSSdist), na.rm=TRUE),
		genomic_partitions=gp)
	l[[fileid]]=bedmeta
	l

	write(jsonlite::toJSON(l, pretty=TRUE), paste0(outfolder, fileid,".json"))

}