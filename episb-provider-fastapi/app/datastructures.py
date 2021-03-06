from pydantic.dataclasses import dataclass
from typing import List
from pydantic import BaseModel
from datetime import datetime

# type definition for chromosomes
chrom_enum = ['chr1', 'chr2', 'chr3', 'chr4', 'chr5', 'chr6', 'chr7', 'chr8', 'chr9', 'chr10', 'chr11', 'chr12', 'chr13', 'chr14', 'chr15', 'chr16', 'chr17', 'chr18', 'chr19', 'chr20', 'chr21', 'chr22', 'chrX', 'chrY']

class Region(BaseModel):
    id: str
    seg_name: str
    chr: str
    start: int
    end: int

class Experiment(BaseModel):
    name: str
    protocol: str
    cell_type: str
    species: str
    tissue: str
    antibody: str
    treatment: str
    description: str
    
class Segmentation(BaseModel):
    name: str
    region_list: List[str]

class Author(BaseModel):
    family_name: str
    given_name: str
    email: str

class Study(BaseModel):
    author: Author
    manuscript: str
    description: str
    date: str
    
class Annotation(BaseModel):
    regionID: str
    value: float
    experiment: Experiment
    study: Study

class DesignInterface(BaseModel):
    segmentation_name: str
    experiment_name: str
    cell_type: str
    description: str
    annotation_key: str
    annotation_range_start: str
    annotation_range_end: str

class ProviderInterface(BaseModel):
    name: str
    institution: str
    admin: str
    contact: str
    segmentations: bool
    segmentations_no: int
    regions_no: int
    annotations_no: int
    experiments_no: int
