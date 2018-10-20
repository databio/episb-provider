# Epigenome switchboard

## Example queries

1. Translating annotation limits into region sets (Retrieving integrated region sets). For example, give me the set of regions with annotation values above *x* in cell-type *y* but below *a* in cell-types *b*, *c*, and *d*. A more complicated example: a user could request that the system return the set of genomic regions that are annotated as filling the following four critera: First, they have open chromatin in macrophages, but not in other hematopoietic cell types, as defined by the ENCODE cross cell-type chromatin analysis; second, they have H3K27ac in M1 macrophages but not in M2 macrophages, as defined by the BLUEPRINT project data; third, they are polycomb repressed in hematopoietic stem cells, as defined by data from the Roadmap Epigenomics project; and fourth, they are within a set of regions defined as CRISPR-targetable in private lab-specific experimental results.

2. Retrieving segmentations. Given a query genomic region or set of regions, or a particular experiment or set of experiments, the system will be able to return a filtered set of genomic segments that link to the given inputs. For example, a user may wish to query the set of TAD boundaries (found in a particular segmentation provider) associated with an input set of transcription factor binding sites.

3. Retrieving annotations for a given region or region set. For example, the user provides a set of genomic regions and wants to retrieve all annotations for those regions. Biological use cases: given a set of genetic variants from a GWAS study, return the functional annotations across data providers for each disease-associated SNP.
