package com.github.oddodaoddo.sheffieldapp.datareader

import com.github.oddodaoddo.sheffieldapp.datastructures._
import java.nio.file.{Files, Paths}
import java.io._
import java.net.InetAddress

import org.elasticsearch.action.bulk.BulkRequestBuilder
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.transport.client.PreBuiltTransportClient


trait DataLoader[T] {
  def loadData(path:String):T
}

class StudyElasticLoaderConverter extends DataLoader[Unit]
  with LocalBedFileReader
  with LocalFileWriter
  with ElasticSearchWriter {

  // establish elasticsearch connection
  private val esclient:TransportClient = new PreBuiltTransportClient(Settings.EMPTY).
      addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"),9300))

  def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  // path points to the top folder of a LOLA core db member study
  // we expect a file named index.txt may exist, collection.txt may exist as well
  // we expect all bed files to be under regions/ subfolder
  override def loadData(path: String) = {
    val sanitizedPath = if (path.endsWith("/")) path else path+"/"

    if (Files.exists(Paths.get(sanitizedPath+"collection.txt"))) {
      // read in the collection.txt and index.txt files to be able to create a Study and Experiment objects
      val lnStudyLn = read(sanitizedPath+"collection.txt").toList.tail.head
      val lnStudy:Array[String] =
        if (lnStudyLn.indexOf(",") != -1)
          lnStudyLn.split(",")
        else if (lnStudyLn.indexOf("\t") != -1)
          lnStudyLn.split("\t")
        else
          new Array[String](0) // FIXME!! What if comma and tab are not delimeters??
      if (Files.exists(Paths.get(sanitizedPath+"index.txt"))) {
        val lnIndex = read(sanitizedPath+"index.txt").toList.tail //skip first line of index.txt
        val authorInfo:Author = {
          val authorParts = lnStudy(0).split(" ")
          if (authorParts.size == 2)
            Author(authorParts(0), authorParts(1), email="")
          else
            Author(authorParts(0), "","")
        }
        //read all bed files and turn into Experiment objects
        val regionsBasePath:String = sanitizedPath+"regions"
        // get all files in regions/ subdirectory by following index.txt
        lnIndex.tail.map(ln => {
          val lnSplit =
            if (ln.indexOf(",") != -1)
              ln.split(",")
            else if (ln.indexOf("\t") != -1)
              ln.split("\t")
            else new Array[String](0) // FIXME!! Possibility of non comma, non tab split??
          val bedFilePath = regionsBasePath+"/"+lnSplit(0)
          println(s"Processing ${bedFilePath}")
          val bedFileLines:List[Option[(String,Int,Int,Option[String])]] = if (Files.exists(Paths.get(bedFilePath))) {
            // here we have processed each line of the bed file into an Option
            readFile(bedFilePath).toList
          } else
            List.empty[Option[(String,Int,Int,Option[String])]]
          val anns:List[Annotation] = bedFileLines.map(bfl => {
            bfl match {
              case Some(x) => {
                Annotation(Segment(x._1.slice(3, x._1.size), x._2, x._3), {
                  x._4 match {
                    case Some(y) => y
                    case None => ""
                  }
                })
              }
              case None => Annotation(Segment("0",0,0),"")
            }
          }) // here we have a list of annotation objects ready to put into elastic
          // generate a valid json string from experiment
          val s:Study = Study(authorInfo,lnStudy(2),lnStudy(3),lnStudy(1))
          val e:Experiment = Experiment(protocol=lnSplit(2),cellType=lnSplit(1),s,anns)
          val json: String = e.toJsonLD
          // commit to json file on disk
          write(bedFilePath+".jsonld", json)
          write(esclient, json)
        })
        esclient.close()
      }
    }
  }
}
