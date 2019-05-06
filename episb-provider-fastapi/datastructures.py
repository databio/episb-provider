from pydantic.dataclasses import dataclass
from typing import L:ist
from pydantic import BaseModel
from datetime import datetime

@dataclass
class Region(BaseModel):
    id: str
    chr: int
    start: int
    end: int

@dataclass
class Experiment(BaseModel):
    name: str
    protocol: str
    cell_type: str
    species: str
    tissue: str
    antibody: str
    treatment: str
    description: str
    
@dataclass
class Segmentation(BaseModel):
    name: str
    region_list: List[str]

@dataclass
class Author(BaseModel):
    family_name: str
    given_name: str
    email: str

@dataclass
class Study(BaseModel):
    author: Author
    manuscript: str
    description: str
    date: datetime
    
@dataclass
class Annotation(BaseModel):
    regionID: str
    value: float
    experiment: Experiment
    study: Study

@dataclass
class DesignInterface(BaseModel):
    segmentation_name: str
    experiment_name: str
    cell_type: str
    description: str
    annotation_key: str
    annotation_range_start: str
    annotation_range_end: str

@dataclass
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
