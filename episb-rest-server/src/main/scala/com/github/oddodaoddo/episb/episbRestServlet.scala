package com.github.oddodaoddo.episb

import org.scalatra._
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.{QueryBuilders, RangeQueryBuilder, BoolQueryBuilder}
import org.elasticsearch.index.query.QueryBuilders._

class episbRestServlet extends ScalatraServlet {

  val esclient = ElasticConnector.getESClient("localhost", 9300)

  get("/get/fromSegment/:start/:end") {
    val segStart:Int = params("start").toInt
    val segEnd:Int = params("end").toInt

    println(s"segStart=${segStart}")
    println(s"segEnd=${segEnd}")

    if (segStart>=segEnd)
      """{ "result" : "None", "error" : "segStart>=segEnd" }"""
    else {

      val range1 = new RangeQueryBuilder("annotationList.Segment.segStart").gte(segStart).lte(segEnd)
      //val range2 = new RangeQueryBuilder("annotationList.Segment.segEnd").gte(segStart).lte(segEnd)
      //val boolQ = new BoolQueryBuilder().must(range1).must(range2)

      // prepare an elastic query
      val response: SearchResponse = esclient.prepareSearch("experiments").
        setQuery(range1).
        setPostFilter(range1).
        //setPostFilter(QueryBuilders.rangeQuery("annotationList.Segment.segEnd").gte(segStart)).
        //setPostFilter(QueryBuilders.rangeQuery("annotationList.Segment.segEnd").lt(segEnd)).
        //setFrom(0).setSize(100).setExplain(true).get
        execute.actionGet

      val resp = response.toString
      println(resp)
      resp
    }
  }
}
