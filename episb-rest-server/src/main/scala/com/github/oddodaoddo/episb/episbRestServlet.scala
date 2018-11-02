package com.github.oddodaoddo.episb

import java.net.InetAddress

import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

import org.scalatra._
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders, RangeQueryBuilder}
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.elasticsearch.common.settings.Settings

object ElasticConnector {
  def getESClient(host: String, port: Int): Either[String,TransportClient] = {
    try {
      val settings:Settings = Settings.builder().put("cluster.name", "episb-elastic-cluster").build()
      Right(new PreBuiltTransportClient(settings).
        addTransportAddress(new TransportAddress(InetAddress.getByName(host), port)))
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
    println(request.body)
  }
}
