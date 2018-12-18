package com.github.oddodaoddo.sheffieldapp.datastructures

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

// this is a class defining a whole study
// a Study consists of Experiment, Author, Annotations, Segmentations, Segments etc.

trait JSONLDable extends java.io.Serializable {
  def partialJsonLD:JObject

  def toJsonLD:String = {
    val finalJson:JObject =
      ("@context" -> "http://bioterms.org/schemas/bioterms.jsonld") ~
      partialJsonLD
    compact(render(partialJsonLD))
  }
}

case class Segment(id:String, chr:String, start:Int, end:Int) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("segID" -> id) ~
    ("segChr" -> chr) ~
    ("segStart" -> start) ~
    ("segEnd" -> end)
  }
}

case class Annotation(segment:Segment,
                      value:String,
                      sptag:String,
                      experiment:Experiment,
                      study:Study) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("annValue" -> value) ~
    ("sptag" -> sptag) ~
    ("Segment" -> segment.partialJsonLD) ~
    ("experiment" -> experiment.partialJsonLD) ~
    ("study" -> study.partialJsonLD)
  }
}

case class Segmentation(name:String, segments:List[Segment]) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("segmentationName" -> name) ~
    ("segmentList" -> segments.map(s => s.partialJsonLD))
  }
}

case class Author(fname:String, lname:String, email:String) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("familyName" -> fname) ~
    ("givenName" -> lname) ~
    ("email" -> email)
  }
}

case class Study(author:Author, manuscript:String, description:String, date:String)
  extends JSONLDable {
  override def partialJsonLD: JObject = {
      ("studyAuthor" -> author.partialJsonLD) ~
      ("studyManuscript" -> manuscript) ~
      ("studyDescription" -> description) ~
      ("studyDate" -> date)
  }
}

case class Experiment(name:String, protocol:String,cellType:String,species:String,
                      tissue:String,antibody:String,treatment:String,description:String) extends JSONLDable {
  override def partialJsonLD: JObject = {
      ("experimentName" -> name) ~
      ("experimentProtocol" -> protocol) ~
      ("experimentCellType" -> cellType) ~
      ("experimentSpecies" -> species) ~
      ("experimentTissue" -> tissue) ~
      ("experimentAntibody" -> antibody) ~
      ("experimentTreatment" -> treatment) ~
      ("experimentDescription" -> description)
  }
}
