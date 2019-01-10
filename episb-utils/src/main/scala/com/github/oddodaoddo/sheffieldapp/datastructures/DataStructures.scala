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

// these are to be able to translate the hits returned from elastic search
// elastic returns JSON that has the number of hits for a search in it, the time it took
// to perform the operation, the quality score of the search, so on and so on
case class hitSegment(_index:String,_type:String,_id:String,_score:Int,_source:Segment)
case class HitsSegment(total:Int,max_score:Int,hits:List[hitSegment])

case class Segment(segID:String, segChr:String, segStart:Int, segEnd:Int) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("segID" -> segID) ~
    ("segChr" -> segChr) ~
    ("segStart" -> segStart) ~
    ("segEnd" -> segEnd)
  }

  //def segmentationName:String = segID.split(":")(0)
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

case class Segmentation(segmentationName:String, segmentList:List[String]) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("segmentationName" -> segmentationName) ~
    ("segmentList" -> segmentList)
  }
}

case class Author(familyName:String, givenName:String, email:String) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("familyName" -> familyName) ~
    ("givenName" -> givenName) ~
    ("email" -> email)
  }
}

case class Study(studyAuthor:Author, studyManuscript:String, studyDescription:String, studyDate:String)
  extends JSONLDable {
  override def partialJsonLD: JObject = {
      ("studyAuthor" -> studyAuthor.partialJsonLD) ~
      ("studyManuscript" -> studyManuscript) ~
      ("studyDescription" -> studyDescription) ~
      ("studyDate" -> studyDate)
  }
}

case class Experiment(experimentName:String,
                      experimentProtocol:String,
                      experimentCellType:String,
                      experimentSpecies:String,
                      experimentTissue:String,
                      experimentAntibody:String,
                      experimentTreatment:String,
                      experimentDescription:String) extends JSONLDable {
  override def partialJsonLD: JObject = {
      ("experimentName" -> experimentName) ~
      ("experimentProtocol" -> experimentProtocol) ~
      ("experimentCellType" -> experimentCellType) ~
      ("experimentSpecies" -> experimentSpecies) ~
      ("experimentTissue" -> experimentTissue) ~
      ("experimentAntibody" -> experimentAntibody) ~
      ("experimentTreatment" -> experimentTreatment) ~
      ("experimentDescription" -> experimentDescription)
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
