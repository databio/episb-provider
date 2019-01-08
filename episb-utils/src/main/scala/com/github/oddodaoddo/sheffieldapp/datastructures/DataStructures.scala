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

case class Segment(segID:String, segChr:String, segStart:Int, segEnd:Int) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("segID" -> segID) ~
    ("segChr" -> segChr) ~
    ("segStart" -> segStart) ~
    ("segEnd" -> segEnd)
  }

  override def toString:String = s"${segID}|${segChr}|${segStart}|${segEnd}"
}

case class Annotation(segmentID:String,
                      annValue:String,
                      experiment:Experiment,
                      study:Study) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("segmentID" -> segmentID) ~
    ("annValue" -> annValue) ~
    ("experiment" -> experiment.partialJsonLD) ~
    ("study" -> study.partialJsonLD)
  }
}

case class Segmentation(segmentationName:String, segmentList:List[Segment]) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("segmentationName" -> segmentationName) ~
    ("segmentList" -> segmentList.map(s => s.partialJsonLD))
  }
}

case class CompressedSegmentation(segmentationName:String, compressedSegments:String) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("segmentationName" -> segmentationName) ~
    ("compressedSegments" -> compressedSegments)
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

case class DesignInterface(providerName:String,
                           providerDescription: String,
                           segmentationName: String,
                           experimentName: String,
                           cellType: String,
                           description: String,
                           annotationKey: String,
                           annotationRangeStart: String,
                           annotationRangeEnd: String) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("providerName" -> providerName) ~
    ("providerDescription" -> providerDescription) ~
    ("segmentationName" -> segmentationName) ~
    ("experimentName" -> experimentName) ~
    ("cellType" -> cellType) ~
    ("description" -> description) ~
    ("annotationKey" -> annotationKey) ~
    ("annotationRangeStart" -> annotationRangeStart) ~
    ("annotationRangeEnd" -> annotationRangeEnd)
  }
}
