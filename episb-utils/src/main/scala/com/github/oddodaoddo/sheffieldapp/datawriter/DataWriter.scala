package com.github.oddodaoddo.sheffieldapp.datareader

import java.io.{FileWriter, IOException}
import java.net.InetAddress

import com.typesafe.config._
import com.typesafe.scalalogging.LazyLogging

import org.json4s.native.JsonMethods._

import com.github.oddodaoddo.sheffieldapp.datastructures.JSONLDable

import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.elasticsearch.common.settings.Settings

trait ElasticConnector {
  // read config file
  private val conf = ConfigFactory.load()
  private val settings:Settings = Settings.builder().
    put("cluster.name", conf.getString("episb-utils.elastic-cluster-name")).build()
  val esclient = new PreBuiltTransportClient(settings).
    addTransportAddress(new TransportAddress(InetAddress.
      getByName(conf.getString("episb-utils.elastic-host")),
      conf.getInt("episb-utils.elastic-port")))
}

trait JSONWriter extends LazyLogging {
  def write(data:String):Either[String,Boolean]
  def write(data:List[JSONLDable]):Either[String,Boolean]
}

class ElasticSearchWriter(index:String, subIndex:String) extends JSONWriter with ElasticConnector {
  def write(data:String):Either[String,Boolean] = {
    try {
      //logger.debug(s"(ElasticSearchWriter::write): data=${data}")
      val esresponse:IndexResponse = esclient.prepareIndex(index, subIndex).
            setSource(data, XContentType.JSON).get()
      Right(true)
    } catch {
      case e:Exception => Left(e.getMessage)
    }
  }

  // write any JSONLDable class descendent into elastic
  // return how many items were written
  def write(data:List[JSONLDable]):Either[String,Boolean] = {
    //logger.info(s"(ElasticSearchWriter::write): data=${data}")
    if (data.isEmpty) Left("Empty list, skipped commital")
    else {
      try {
        val bulkReq = esclient.prepareBulk
        data.foreach(x => bulkReq.
          add(esclient.prepareIndex(index, subIndex).
            setSource(compact(render(x.partialJsonLD)),XContentType.JSON)))
        val bulkRes = bulkReq.get
        if (bulkRes.hasFailures) 
          Left("Problem writing data into elastic")
        else
          Right(true)
        Right(true)
      } catch {
        case e:Exception => Left(e.getMessage)
      }
    }
  }
}

// creates new file on every class instantiation
class LocalFileWriter(path:String) extends JSONWriter {
  def write(data:String): Either[String, Boolean] = {
    //logger.debug(s"(LocalFileWriter::write): data=${data}")
    try {
      val f: FileWriter = new FileWriter(path)
      f.write(data) // write to local file
      f.close
      Right(true)
    } catch {
        case ioe: IOException => Left(ioe.getMessage)
    }
  }

  def write(data:List[JSONLDable]):Either[String,Boolean] = {
    //logger.debug(s"(LocalFileWriter::write): data=${data}")
    if (data.isEmpty) Left("Empty list, skipped commital")
    else {
      write(data.map(_.toJsonLD).mkString("\n"))
    }
  }
}
