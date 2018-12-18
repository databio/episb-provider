package com.github.oddodaoddo.episb

import java.math.BigInteger
import java.net.InetAddress

import com.typesafe.config.ConfigFactory
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
import org.scalatra._
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders, RangeQueryBuilder, TermQueryBuilder}
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.elasticsearch.common.settings.Settings
import org.json4s.JsonAST.JNothing

object ElasticConnector {
  def getESClient(host: String, port: Int): Either[String,TransportClient] = {
    try {
      val conf = ConfigFactory.load()
      val settings:Settings = Settings.builder().
        put("cluster.name", conf.getString("episb-rest-server.elastic-cluster-name")).build()
      Right(new PreBuiltTransportClient(settings).
        addTransportAddress(new TransportAddress(InetAddress.
        getByName(conf.getString("episb-rest-server.elastic-host")),
          conf.getInt("episb-rest-server.elastic-port"))))
    } catch {
      case e: Exception => Left(s"Could not establish connection to Elastic cluster. Reason: ${e}")
    }
  }
}

case class JsonError(reason:String) {
  override def toString:String = {
    val json = ("result" -> "None") ~
      ("error" -> reason)
    compact(render(json))
  }
}

class episbRestServlet(esclient:TransportClient) extends ScalatraServlet {

  get("/get/fromSegment/:start/:end") {
    // get the parameters to the query
    val segStart:Int = params("start").toInt
    val segEnd:Int = params("end").toInt

    if (segStart>segEnd)
      JsonError(s"segStart(${segStart}) > segEnd(${segEnd})").toString
    else {
      val range1 = new RangeQueryBuilder("Segment.segStart").gte(segStart).lte(segEnd)
      val range2 = new RangeQueryBuilder("Segment.segEnd").gte(segStart).lte(segEnd)

      // prepare an elastic query
      val response: SearchResponse = esclient.prepareSearch("annotations").
        setQuery(range1).
        setPostFilter(range2).
        setSize(10000).
        get

      response.toString
    }
  }

  post("/post/fromSegmentSet") {
    def queueSearchRequests(s:SearchRequestBuilder, rs:List[RangeQueryBuilder]):SearchRequestBuilder =  {
      rs.foreach(x => s.setPostFilter(x))
      s
    }

    val json = parse(request.body)
    if (json \ "segmentSet" == JNothing)
      "error!"
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

  get("/segmentation/match/exact/:chr/:start/:end") {
    val segStart:Int = params("start").toInt
    val segEnd:Int = params("end").toInt
    val chr:String = params("chr")
    
    if (segStart>segEnd)
      JsonError(s"segStart(${segStart}) > segEnd(${segEnd})").toString
    else {
      try {
        val startQuery = QueryBuilders.termQuery("Segment.segStart", segStart)
        val endQuery = QueryBuilders.termQuery("Segment.segEnd", segEnd)
        val chrQuery = QueryBuilders.termsQuery("Segment.segChr", chr)

        val qb = QueryBuilders.boolQuery
        qb.must(startQuery).must(endQuery).must(chrQuery)

        // prepare an elastic query
        val response: SearchResponse = esclient.prepareSearch("segments").
          setQuery(qb).setSize(1).get

        response.toString
      } catch {
        case e:Exception => JsonError(e.getMessage)
      }
    }
  }

  get("/segmentation/get/ByNameWithSegments/:segName") {
    val segName = params("segName")
    try {
      val qb = QueryBuilders.termQuery("segmentation.segmentationName", segName)
      val response = esclient.prepareSearch("segmentations").
        setQuery(qb).setSize(1).get
      response.toString
    } catch {
      case e:Exception => JsonError(e.getMessage)
    }
  }
}
