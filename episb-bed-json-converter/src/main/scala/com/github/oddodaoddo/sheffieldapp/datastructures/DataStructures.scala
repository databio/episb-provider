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

case class Segment(chr:String, start:Int, end:Int) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("segChr" -> chr) ~
    ("segStart" -> start) ~
    ("segEnd" -> end)
  }
}

case class Annotation(segment:Segment, value:String) extends JSONLDable {
  override def partialJsonLD: JObject = {
    ("annValue" -> value) ~
    ("Segment" -> segment.partialJsonLD)
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

case class Experiment(protocol:String,cellType:String,study:Study,anns:List[Annotation]) extends JSONLDable {
  override def partialJsonLD: JObject = {
      ("experimentProtocol" -> protocol) ~
      ("experimentCellType" -> cellType) ~
      ("study" -> study.partialJsonLD) ~
      ("annotationList" -> anns.map(a => a.partialJsonLD))
  }
}
