package com.github.oddodaoddo.sheffieldapp.datareader

import java.io.FileWriter

import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentType

trait DataWriter[T] extends java.io.Serializable {
  def write(path:String, data:T)
  def write(path:TransportClient, data:T)
}

trait LocalFileWriter extends DataWriter[String] {
  def write(path:String, data:String)= {
    val f:FileWriter = new FileWriter(path)
    f.write(data) // write to local file
    f.close
  }
}

trait ElasticSearchWriter extends DataWriter[String] {
  def write(path:TransportClient, data:String) = {
    val esresponse:IndexResponse = path.prepareIndex("experiments","experiment").
            setSource(data, XContentType.JSON).get()
  }
}
