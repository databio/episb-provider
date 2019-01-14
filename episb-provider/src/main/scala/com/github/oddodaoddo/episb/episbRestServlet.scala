package com.github.oddodaoddo.episb

import org.scalatra.{ContentEncodingSupport, ScalatraServlet}
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import org.scalatra.scalate.ScalateSupport

import java.math.BigInteger
import java.net.InetAddress

import com.typesafe.config.ConfigFactory

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
import org.json4s.JsonAST.JNothing

import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.search.sort.{SortBuilder, SortBuilders}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders, RangeQueryBuilder, TermQueryBuilder}
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.elasticsearch.common.settings.Settings

import com.typesafe.scalalogging.LazyLogging

import com.github.oddodaoddo.sheffieldapp.util._
import com.github.oddodaoddo.sheffieldapp.datareader._
import com.github.oddodaoddo.sheffieldapp.datastructures._

case class JsonError(reason:String) {
  override def toString:String = {
    val json = ("result" -> "None") ~
      ("error" -> reason)
    compact(render(json))
  }
}

case class JsonSuccess(result:List[JSONLDable]) {
  override def toString:String = {
    val json = if (result.isEmpty) {
      (("result" -> "Ok") ~ ("error" -> "None"))
    } else {
      (("result" -> result.map(r => r.partialJsonLD)) ~ ("error" -> "None"))
    }
    compact(render(json))
  }
}

class episbRestServlet extends ScalatraServlet 
    with ElasticConnector 
    with FileUploadSupport
    with ScalateSupport
    with ContentEncodingSupport 
    with LazyLogging {

  implicit val formats = DefaultFormats

  // set physical limit on file size uploads (500Mb) - arbitrary right now
  // maybe in the future we support .gz to allow a bigger file upload
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(500*1024*1024)))
  
  // first API point: get segments for a range of start/end
  get("/get/fromSegment/:chr/:start/:end") {

    // get the parameters to the query
    val segChr:String = {
      // if chromosome coordinate starts with "chr"
      // remove the "chr" portion
      val c = params("chr").toLowerCase
      if (c.startsWith("chr"))
        c.substring(3,c.length)
      else
        c
    }
    val segStart:Int = params("start").toInt
    val segEnd:Int = params("end").toInt

    if (segStart>segEnd)
      JsonError(s"segStart(${segStart}) > segEnd(${segEnd})").toString
    else {
      val range1 = new RangeQueryBuilder("segStart").gte(segStart).lte(segEnd)
      val range2 = new RangeQueryBuilder("segEnd").gte(segStart).lte(segEnd)
      val chrQuery = QueryBuilders.matchQuery("segChr", segChr)

      val totalQuery = QueryBuilders.boolQuery.must(chrQuery)

      // first we need to get all the segments IDs that match the start/end range
      val response: SearchResponse = esclient.prepareSearch("regions").
        setQuery(totalQuery).
        setPostFilter(range1).
        setPostFilter(range2).
        setSize(100).
        get

      // get the compressed segmentation out of a normal segmentation
      // FIXME: assumes segments exist (for now)
      val hs:Option[HitsSegment] = try {
        val j = parse(response.toString)
        // deconstruct the elastcsearch response
        val h = (j \ "hits").extract[HitsSegment]
        // get the compressed segmentation now
        Some(h)
      } catch {
        case e:Exception => None
      }

      if (hs.isDefined) {
        // get the actual list of segments
        val segments:List[Segment] = hs.get.hits.map(_._source)
        JsonSuccess(segments)
      } else 
          JsonError("No segments found with that range")
    }
  }

/*
  post("/post/fromSegmentSet") {
    def queueSearchRequests(s:SearchRequestBuilder, rs:List[RangeQueryBuilder]):SearchRequestBuilder =  {
      rs.foreach(x => s.setPostFilter(x))
      s
    }

    val json = parse(request.body)
    if (json \ "segmentSet" == JNothing)
      JsonError("No Segment Set section in request body")
    else {
      // get all segStart,segEnd pairs
      val sset:List[Map[String,BigInt]] = (json \ "segmentSet").values.asInstanceOf[List[Map[String,BigInt]]]
      // take only the segmentSets that have a segStart/segEnd pair and where segEnd>segStart
      val filtered_sset:List[Map[String,Int]] = sset.filter(x => x.contains("segStart") && x.contains("segEnd")).
        filter(x => x("segEnd").intValue > x("segStart").intValue).
        map(x => Map("segStart" -> x("segStart").intValue, "segEnd"-> x("segEnd").intValue))
      // prepare the elastic compound query
      val responses:List[(Int,Int,String)] = filtered_sset.map(x => (x("segStart"),x("segEnd"),{
        val range1 = new RangeQueryBuilder("Segment.segStart").gte(x("segStart")).lte(x("segEnd"))
        val range2 = new RangeQueryBuilder("Segment.segEnd").gte(x("segStart")).lte(x("segEnd"))
      // prepare an elastic query
      esclient.prepareSearch("annotations").
        setQuery(range1).
        setPostFilter(range2).
        setSize(100).
        get.toString
      }))
      compact(render(responses.map(x => {
        ("segStart" -> x._1) ~ ("segEnd" -> x._2) ~("response" -> x._3)
      })))
    }
  }
*/

  // get the exactly matching segment from segments index
  // FIXME: change analyzers on regions index so that we can match
  //        strings properly!
  get("/segments/find/BySegmentID/:segID") {
    val segID:String = params("segID")
    try {
      // we are using a regex query on the first part of the UUID below
      // this is because elastic cannot search on terms that have special characters in them
      // at least not without special analyzers (which we can add later)
      val qb = QueryBuilders.regexpQuery("segID", segID.split(":")(2).split("-")(0))

      // prepare an elastic query
      val response: SearchResponse = esclient.prepareSearch("regions").
        setQuery(qb).setSize(1).get

      // we may get more than one hit so we have to search manually ourselves
      // fortunately the search space will be very small
      val hs:Option[HitsSegment] = try {
        val j = parse(response.toString)
        // get the segmentation from the response
        val h = (j \ "hits").extract[HitsSegment]
        Some(h)
      } catch {
        case e:Exception => None
      }

      if (hs.isDefined) {
        val segments:List[Segment] = hs.get.hits.map(_._source)
        val segPos = segments.indexWhere(s => s.segID==segID)
        if (segPos != -1)
          JsonSuccess(List(segments(segPos)))
        else
          JsonError(s"No segments found with ID ${segID}")
      } else 
          JsonError(s"No segments found with ID ${segID}")
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }

  // get the segmentation with name parameter "segName"
  // FIXME: strip out elasticsearch response
  get("/segmentations/get/ByNameWithSegments/:segName") {
    val segName = params("segName")
    //val sorted
    try {
      val qb = QueryBuilders.matchQuery("segmentationName", segName)
      val response = esclient.prepareSearch("segmentations").
        setQuery(qb).setSize(1).get
      response.toString
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }


  // get all segments in a segmentation (with chr/start/stop data included)
  // FIXME: strip out all elasticsearch query data and just return pure segments
  get("/segments/get/BySegmentationName/:segName") {
    val segName = params.getOrElse("segName", "defaultSegmentation").toLowerCase

    try {
      val qb = QueryBuilders.regexpQuery("segID", segName+".*")
      val response = esclient.prepareSearch("regions").
        setQuery(qb).setSize(10000).get

      val hs:Option[HitsSegment] = try {
        val j = parse(response.toString)
        // deconstruct the elasticsearch response
        val h = (j \ "hits").extract[HitsSegment]
        Some(h)
      } catch {
        case e:Exception => None
      }

      if (hs.isDefined) {
        // get the list of segments from the actual elasticsearch response
        val segments:List[Segment] = hs.get.hits.map(_._source)
        JsonSuccess(segments)
      } else 
          JsonError("No segments found with that range")

    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }

  // this api point adds a pre-prepared experiment file with the following format
  // annotation_value<tab>segmentationName::segmentID
  // this file can be prepared on the command line using our utility
  // from episb-bed-json-converter
  post("/experiments/add/preformatted/:expname/:segmname") {
    // we also need the name of the bed file
    val expName = params("expname")
    val segmName = params("segmname")

    // get the file
    val expfile  = fileParams("expfile")

    logger.info(s"(/experiments/add/preformatted):: expName=${expName}, segmName=${segmName}, expFile=${expfile}")

    // get a connection to elasticsearch
    val elasticWriter = new ElasticSearchWriter("annotations", "annotation")

    // now process it, assumes \n delimited lines
    val input = expfile.get.map(x => x.toChar).mkString("")
    val lines:List[Array[String]] = input.split("\n").toList.map(_.split("\t"))
    val exp:Experiment = Experiment(expName,"","","","","","","Loaded from preformatted file")
    val study:Study = Study(Author("episb","default","info@episb.org"),"","","")
    val annotations:List[Annotation] = lines.map(ln => Annotation(ln(1), ln(0), exp, study))

    // write those into elastic (for now)
    // FIXME: no error handling!
    elasticWriter.write(annotations)

    JsonSuccess
  }

  // add an experiment to segmentations list  in elastic
  post("/segmentations/update") {
    // get a connection to elasticsearch
    val elasticWriter = new ElasticSearchWriter("interfaces", "interface")

    // FIXME:: and write the received json verbatim
    elasticWriter.write(request.body)

    JsonSuccess
  }

  // get all "design interfaces" from the provider
  get("/segmentations/get/all") {      
    val response = esclient.prepareSearch("interfaces").setSize(1000).get

    try {
      val j = (parse(response.toString) \ "hits").extract[HitsDesignInterface]
      val dis:List[DesignInterface] = j.hits.map(_._source)
      JsonSuccess(dis)
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }

  // return all annotation values for a particular experiment
  get("/experiments/get/ByName/:expName") {
    val expName:String = params("expName").toLowerCase
    try {
      val qb = QueryBuilders.regexpQuery("experiment.experimentName", expName)

      // prepare an elastic query
      val response: SearchResponse = esclient.prepareSearch("annotations").
        setQuery(qb).setSize(10000).get

      val hs:Option[HitsAnnotation] = try {
        val j = parse(response.toString)
        // get the annotations from the response
        val h = (j \ "hits").extract[HitsAnnotation]
        Some(h)
      } catch {
        case e:Exception => None
      }

      if (hs.isDefined) {
        // extract the actual list of annotations from the elastic search response
        val annotations:List[Annotation] = hs.get.hits.map(_._source)
        JsonSuccess(annotations)
      } else 
          JsonError(s"No annotations found belonging to experiment with name ${expName}")
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }

  /*get("/experiments/get/BySegmentationName/:segName") {
    val segName:String = params("segName").toLowerCase
    try {
      // we are using a regex query on the first part of the UUID below
      // this is because elastic cannot search on terms that have special characters in them
      // at least not without special analyzers (which we can add later)
      val qb = QueryBuilders.regexpQuery("segmentationName", segName.split(":")(0))

      // prepare an elastic query
      val response: SearchResponse = esclient.prepareSearch("annotations").
        setQuery(qb).setSize(1).get

      // we may get more than one hit so we have to search manually ourselves
      // fortunately the search space will be very small
      val hs:Option[HitsAnnotation] = try {
        val j = parse(response.toString)
        // get the segmentation from the response
        val h = (j \ "hits").extract[HitsAnnotation]
        Some(h)
      } catch {
        case e:Exception => None
      }

      if (hs.isDefined) {
        // we have a compressed segmentation to work with
        val annotations:List[Annotation] = hs.get.hits.map(_._source)
        JsonSuccess(annotations)
      } else 
          JsonError(s"No annotations found belonging to experiment with name ${expName}")
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }*/
}
