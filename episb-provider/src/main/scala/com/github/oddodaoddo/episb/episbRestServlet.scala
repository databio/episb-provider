package com.github.oddodaoddo.episb

import org.scalatra.{ContentEncodingSupport, ScalatraServlet}
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import org.scalatra.scalate.ScalateSupport

import java.math.BigInteger
import java.net.InetAddress
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.util.UUID.randomUUID

import com.typesafe.config.ConfigFactory

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
import org.json4s.JsonAST.JNothing

import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.search.sort.{SortBuilder, SortBuilders}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.index.query.{BoolQueryBuilder, MatchQueryBuilder, QueryBuilders, RangeQueryBuilder, TermQueryBuilder}
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.unit.TimeValue

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

// FIXME: ugly hack, violates FP principles, get rid of it!
case class JsonSuccessBasic(result:List[String]) {
  override def toString:String = {
    val json = if (result.isEmpty) {
      (("result" -> "Ok") ~ ("error" -> "None"))
    } else {
      (("result" -> result) ~ ("error" -> "None"))
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

  // get all provider-interface info
  // read config file
  private val conf = ConfigFactory.load()
  private val providerName = conf.getString("episb-provider.provider-name")
  private val providerDescription = conf.getString("episb-provider.provider-description")
  private val providerInstitution = conf.getString("episb-provider.provider-institution")
  private val providerAdmin = conf.getString("episb-provider.provider-admin")
  private val providerAdminContact = conf.getString("episb-provider.provider-contact")
  private val segmentationsProvided = conf.getBoolean("episb-provider.segmentation-provider")
 
  get("/") {
    contentType="text/html"

    <html>
      <head><title>EPISB landing page</title></head>
      <body>EPISB provider landing page.
            Try appending /provider-interface to the URL or go to <a href="http://episb.org">EPISB.ORG</a> for more information.
      </body>
    </html>
  }

  // first API point: get segments for a range of start/end
  // allow for ?format=[bed/json] as return type of query
  get("/segments/get/fromSegment/:chr/:start/:end") {
    val fmt = params.getOrElse("format", "json").toLowerCase

    // get the parameters to the query
    val segChr:String = {
      // if chromosome coordinate starts with "chr"
      // remove the "chr" portion
      val c = params("chr").toLowerCase.replace(" ", "")
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

      // FIXME: assumes segments exist (for now)
      val hs:Option[HitsSegment] = try {
        val j = parse(response.toString)
        // deconstruct the elastcsearch response
        val h = (j \ "hits").extract[HitsSegment]
        Some(h)
      } catch {
        case e:Exception => None
      }

      if (hs.isDefined) {
        // get the actual list of segments
        val segments:List[Segment] = hs.get.hits.map(_._source)
        if (fmt == "bed")
          segments.map(_.toBed).mkString("\n")
        else
          JsonSuccess(segments)
      } else 
          JsonError("No segments found with that range")
    }
  }

  // get the exactly matching segment from segments index
  // FIXME: change analyzers on regions index so that we can match
  //        strings properly!
  // add ?format=[json/bed] argument
  get("/segments/find/BySegmentID/:segID") {
    val segID:String = params("segID")
    val fmt = params.getOrElse("format", "json").toLowerCase

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
        if (segPos != -1) {
          if (fmt == "bed")
            segments(segPos).toBed
          else
            JsonSuccess(List(segments(segPos)))
        } else
          JsonError(s"No segments found with ID ${segID}")
      } else 
          JsonError(s"No segments found with ID ${segID}")
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }

  // get the segmentation with name parameter "segName"
  // FIXME: strip out elasticsearch response
  get("/segmentations/get/ByName/:segName") {
    val segName = params("segName")
    //val sorted
    try {
      val qb = QueryBuilders.matchQuery("segmentationName", segName)
      val response = esclient.prepareSearch("segmentations").
        setQuery(qb).setSize(1).get
      val hs:Option[HitsSegmentation] = try {
        val j = parse(response.toString)
        // get the segmentation from the response
        val h = (j \ "hits").extract[HitsSegmentation]
        Some(h)
      } catch {
        case e:Exception => None
      }

      if (hs.isDefined) {
        val segmentations:List[Segmentation] = hs.get.hits.map(_._source)
        JsonSuccess(segmentations)
      } else
          JsonError(s"No segmentations found with name ${segName}")
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }

  // get all segments in a segmentation (with chr/start/stop data included)
  // FIXME: strip out all elasticsearch query data and just return pure segments
  // add ?format=[bed/json] parameter
  get("/segments/get/BySegmentationName/:segName") {
    val segName = params.getOrElse("segName", "defaultSegmentation").toLowerCase
    val fmt = params.getOrElse("format", "json").toLowerCase

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
        if (fmt == "bed")
          segments.map(_.toBed).mkString("\n")
        else
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
    val annotations:List[Annotation] = lines.map(ln => Annotation(ln(1), ln(0).toFloat, exp, study))

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

  get("/segmentations/list/all") {
    val response = esclient.prepareSearch("segmentations").setFetchSource(Array("segmentationName"), Array.empty[String]).setSize(1000).get

    try {
      val j = parse(response.toString)
      // a dirty way to extract a list of Strings
      val lst:List[String] = (((j\\"hits")\\"segmentationName") \\ classOf[JString]).toList.distinct.map(_.toString)
      //val j = (parse(response.toString) \ "hits").extract[HitsSegmentation]
      //val dis:List[Segmentation] = j.hits.map(_._source)
      JsonSuccessBasic(lst)
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
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
  // can be passed in parameters like ?op1=gt&val1=0.5
  get("/experiments/get/ByName/:expName") {
    val expName:String = params("expName").toLowerCase
    val op1:Option[String] = { 
      val p = params.getOrElse("op1","").toLowerCase
      if (p != "gte" && p != "lte" && p != "eq")
        None
      else
        Some(p)
    }
    val op2:Option[String] = { 
      val p = params.getOrElse("op2","").toLowerCase
      if (p != "gte" && p != "lte" && p != "eq")
        None
      else
        Some(p)
    }

    val opval1:Option[Double] = if (op1.isDefined) 
      try { 
        Some(params.getOrElse("val1", "").toString.toDouble)
      } catch { 
        case e:Exception => None 
      } else 
          None
    val opval2:Option[Double] = if (op2.isDefined)
      try {
        Some(params.getOrElse("val2", "").toString.toDouble)
      } catch { 
        case e:Exception => None 
      } else 
          None

    try {
      if ((op1.isDefined && !opval1.isDefined) || (op2.isDefined && !opval2.isDefined))
        JsonError("Invalid combination of filter operands and filter values")

      val response:SearchResponse = if (!op1.isDefined && !op2.isDefined) {
        // baskic case where we want ALL annotations from an experiment
        val qb = QueryBuilders.regexpQuery("experiment.experimentName", expName)
        // prepare an elastic query
        // FIXME: limited by scroll size
        esclient.prepareSearch("annotations").setQuery(qb).setSize(10000).get
      } else if (op1.isDefined && op1.get == "eq") {
        val expNameQuery = QueryBuilders.matchQuery("experiment.experimentName", expName)
        val eqQuery = QueryBuilders.matchQuery("annValue", opval1.get)
        val totalQuery = QueryBuilders.boolQuery.must(expNameQuery).must(eqQuery)

        // first we need to get all the segments IDs that match the start/end range
        esclient.prepareSearch("regions").setQuery(totalQuery).get
      } else {
        val range1 = {
          if (op1.isDefined) {
            if (op1.get == "gte")
              Some(new RangeQueryBuilder("annValue").gte(opval1.get))
            else if (op1.get == "lte")
              Some(new RangeQueryBuilder("annValue").lte(opval1.get))
            else
              None
          } else
              None
        }
        val range2:Option[RangeQueryBuilder] = {
          if (op2.isDefined) {
            if (op2.get == "gte")
              Some(range1.get.gte(opval2.get))
            else if (op2.get == "lte")
              Some(range1.get.lte(opval2.get))
            else 
              None
          } else
              None
        }
        val expNameQuery = QueryBuilders.matchQuery("experiment.experimentName", expName)
        val totalQuery = QueryBuilders.boolQuery.must(expNameQuery)
        val query = esclient.prepareSearch("annotations").setQuery(totalQuery)

        if (range2.isDefined)
          query.setPostFilter(range2.get)
        else
          query.setPostFilter(range1.get)

        query.setSize(1000).get
      }
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

  // if ?matrix=true is passed in, get ALL the annotations for a segmentation
  // and serve as a .gz file
  get("/experiments/get/BySegmentationName/:segName") {

    def sequence[A](a: List[Option[A]]): Option[List[A]] =
    a match {
      case Nil => Some(Nil)
      case h :: t => h flatMap (hh => sequence(t) map (hh :: _))
    }

    val segName:String = {
      val p = params("segName").toLowerCase
      if (p.contains("::"))
        p.split("::")(0)
      else
        p
    }

    val matrix:Boolean = try {
      val p = params.getOrElse("matrix", "false")
      p.toBoolean
    } catch {
      case iae: IllegalArgumentException => false
    }

    if (matrix) {
      // non-mutable variables, do/while - yuck!!
      // create a random temporary file first
      val fname:String = randomUUID().toString
      val tempDir:File = new File(System.getProperty("java.io.tmpdir"));
      val tempFile:File = File.createTempFile(fname, ".tmp", tempDir);
      val fileWriter:FileWriter = new FileWriter(tempFile, true);
      val bw:BufferedWriter = new BufferedWriter(fileWriter);

      // here we get to use the elastic scroll API to build the .gz file
      val qb = QueryBuilders.regexpQuery("segmentID", segName + ".*")
      var response = esclient.prepareSearch("annotations").
        setScroll(new TimeValue(60000)).
        setQuery(qb).
        setSize(1000).
        get

      // now we can start building the file on disk
      do {
        //response.getHits.getHits.foreach(println(_))
        // convert each JSON document to a single row of format segmentID:annValue
        val hass:Option[List[hitAnnotation]] = sequence(response.getHits.getHits.toList.map(h => try {
          val j = parse(h.toString)
          // get the segmentation from the response
          val jj = (j).extract[hitAnnotation]
          Some(jj)
        } catch {
          case e:Exception => None
        }))
        val row:Option[List[String]] = hass.map(h => h.map(x => s"${x._source.segmentID},${x._source.annValue}"))
        row.get.foreach(r => bw.write(r + "\n"))
        response = esclient.prepareSearchScroll(response.getScrollId).setScroll(new TimeValue(60000)).execute.actionGet
      } while (response.getHits.getHits.length != 0)

      bw.close
      tempFile
    } else {
      try {
        // we are using a regex query on the first part of the UUID below
        // this is because elastic cannot search on terms that have special characters in them
        // at least not without special analyzers (which we can add later)
        // (FIXME)
        val qb = QueryBuilders.regexpQuery("segmentID", segName + ".*")

        // prepare an elastic query
        // FIXME: another place for scroll API!
        val response: SearchResponse = esclient.prepareSearch("annotations").
          setQuery(qb).setSize(10000).get

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
          val annotations:List[Annotation] = hs.get.hits.map(_._source)
          JsonSuccess(annotations)
        } else
          JsonError(s"No annotations found belonging to experiment with segmentation name ${segName}")
      } catch {
        case e:Exception => JsonError(e.getMessage)
      }
    }
  }

  get("/experiments/list/BySegmentationName/:segName") {
    val response = esclient.prepareSearch("interfaces").setSize(1000).get
    val segName:String = params("segName").toLowerCase

    try {
      val j = (parse(response.toString) \ "hits").extract[HitsDesignInterface]
      val dis:List[DesignInterface] = j.hits.map(_._source)
      JsonSuccessBasic(dis.filter(d => d.segmentationName.toLowerCase == segName).map(_.experimentName))
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }

  get("/experiments/list/full/BySegmentationName/:segName") {
    val response = esclient.prepareSearch("interfaces").setSize(1000).get
    val segName:String = params("segName").toLowerCase

    try {
      val j = (parse(response.toString) \ "hits").extract[HitsDesignInterface]
      val dis:List[DesignInterface] = j.hits.map(_._source)
      JsonSuccess(dis.filter(d => d.segmentationName.toLowerCase == segName))
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }

  get("/experiments/get/ByRegionID/:regionID") {
    val originalParameter = params("regionID")

    // we do some preprocessing on the parameter
    // so the search is easier
    val regionID:Option[String] = {
      // we are going to search on a portion of the region ID
      // the expectation is to get the parameter as segmentation::uuid
      // we want to get one portion of the uuid and search on that
      val r = params("regionID").toLowerCase
      val rr:String = if (r.contains("::"))
        r.split("::")(1)
      else
        r
      if (rr.contains("-"))
        Some(rr.split("-")(0))
      else
        None
    }
    if (regionID.isDefined) {
      try {
        // we are using a regex query on the first part of the UUID below
        // this is because elastic cannot search on terms that have special characters in them
        // at least not without special analyzers (which we can add later)
        val qb = QueryBuilders.regexpQuery("segmentID", regionID.get)

        // prepare an elastic query
        // we will get multiple replies and because we are using regex
        // some of them may not actually have the region ID the user wanted
        // we will have to search those manually
        val response: SearchResponse = esclient.prepareSearch("annotations").
          setQuery(qb).setSize(1000).get

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
          val annotations:List[Annotation] = hs.get.hits.map(_._source)
          JsonSuccess(annotations.filter(a => a.segmentID == originalParameter))
        } else 
          JsonError(s"No annotations found that have a region ID ${originalParameter}")
      } catch {
        case e:Exception => JsonError(e.getMessage)
      }
    } else
        JsonError(s"Invalid parameter ${originalParameter}")
  }

  get("/provider-interface") {
    val segCount:Long = esclient.admin.indices.prepareStats("regions").get.getTotal.getDocs.getCount
    val annCount:Long = esclient.admin.indices.prepareStats("annotations").get.getTotal.getDocs.getCount
    val segmentationCount:Long = esclient.admin.indices.prepareStats("segmentations").get.getTotal.getDocs.getCount
    val expCount:Long = esclient.admin.indices.prepareStats("interfaces").get.getTotal.getDocs.getCount

    val result = ProviderInterface(providerName,
                           providerDescription,
                           providerInstitution,
                           providerAdmin,
                           providerAdminContact,
                           segmentationsProvided,
                           segmentationCount,
                           segCount,
                           annCount,
                           expCount)

    JsonSuccess(List(result))
  }

}
