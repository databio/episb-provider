package com.github.oddodaoddo.sheffieldapp.datareader

import java.io.{FileWriter, IOException}
import java.net.InetAddress

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

import com.github.oddodaoddo.sheffieldapp.datastructures.Annotation
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.elasticsearch.common.settings.Settings

class LocalFileWriter(path:String) {
  def write(data:String): Either[String, Boolean] = {
    try {
      val f: FileWriter = new FileWriter(path)
      f.write(data) // write to local file
      f.close
      Right(true)
    } catch {
        case ioe: IOException => Left(ioe.getMessage)
      }
  }
}

class ElasticSearchWriter(host:String, port:Int) {
  // establish elasticsearch connection
  private val settings:Settings = Settings.builder().put("cluster.name", "episb-elastic-cluster").build()
  private val esclient:TransportClient = new PreBuiltTransportClient(settings).
      addTransportAddress(new TransportAddress(InetAddress.getByName(host),port))

  def elasticWrite(data:String) = {
    val esresponse:IndexResponse = esclient.prepareIndex("annotations","annotation").
            setSource(data, XContentType.JSON).get()
  }

  def elasticWrite(data:List[Annotation]) = {
    val bulkReq = esclient.prepareBulk
    val zippedData:List[(Annotation,Int)] = data.zipWithIndex
    zippedData.foreach(x => bulkReq.
      add(esclient.prepareIndex("annotations", "annotation", x._2.toString).
      setSource(compact(render(x._1.partialJsonLD)),XContentType.JSON)))
   bulkReq.get()
  }
}
