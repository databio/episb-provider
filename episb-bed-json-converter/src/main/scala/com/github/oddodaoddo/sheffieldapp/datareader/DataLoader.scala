package com.github.oddodaoddo.sheffieldapp.datareader

import com.github.oddodaoddo.sheffieldapp.datastructures._
import java.io._
import java.nio.file.{Files, Paths}

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

class StudyElasticLoaderConverter(pathsToLoad:Array[String]) {
  private val esclient = new ElasticSearchWriter("localhost", 9300)

  pathsToLoad.foreach(path => loadData(path))

  // get a list of files from a directory
  // FIXME: for now we assume our data is in LOLACore format
  // FIXME: need more thought on reading in non LOLACore formatted data sources!
  private def getFilesFromDir(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  // either get a list of files from index.txt or get it from regions/
  // by reading the directory
  // commented out for now until we can come up with good stragegy for non LOLACore sources
  // FIXME:
  /*def getListOfFilesToProcess(sanitizedPath:String):List[File] = {
    if (Files.exists(Paths.get(sanitizedPath + "index.txt")))
      read(sanitizedPath + "index.txt").toList.tail.
        map(ln => splitDelimitedLine(ln)).flatten.map(x => new File(x(0)))
    else
      getFilesFromDir(sanitizedPath+"/regions")
  }*/

  // map the name of the column to the position in a file line
  /*def getHeadersFromLOLAIndexFile(sanitizedPath:String):Map[String,Int] = {

  }*/

  // all bed files are assumed to live under path/regions/ subfolder
  // path points to the top folder of a LOLA core db member study
  // we expect a file named index.txt may exist, collection.txt may exist as well
  // if index.txt does not exist, we will attempt and read the regions/ subfolder as it is
  def loadData(path:String) = {
    val sanitizedPath = if (path.endsWith("/")) path else path+"/"
    // get author info
    val study:Study = new LocalCollectionFile(sanitizedPath + "collection.txt").
      getStudyFromLOLACollectionFile
    // get study and experiment info
    val indexFile = new LocalIndexFile(sanitizedPath + "index.txt")
    val expMap:Map[String,Experiment] = indexFile.getExperimentFromLOLAIndexFile
    // get list of bed files to process
    indexFile.fileList.
      foreach(bedFileName => {
        val absbedFilePath = sanitizedPath + "regions/" + bedFileName
        if (Files.exists(Paths.get(absbedFilePath))) {
          println(s"Processing ${absbedFilePath}")
          val bedFileLines: List[Array[String]] =
            if (Files.exists(Paths.get(absbedFilePath))) {
              val f = new LocalFileReader(absbedFilePath)
              f.read.toList.map(ln => f.splitDelimitedLine(ln))
            } else
              List.empty
          val anns: List[Annotation] = bedFileLines.map(bfl =>
            Annotation(Segment(bfl(0).slice(3, bfl(0).size), bfl(1).toInt, bfl(2).toInt), {
              if (bfl.size > 3) bfl(3) else ""
            }, expMap(bedFileName), study))
          esclient.elasticWrite(anns)
        }
      }) // here we have a list of annotation objects ready to put into elastic
      // we still need to populate the Study and Experiment objects here
      // generate a valid json string from experiment
      // commit to json file on disk
      //fileWrite(bedFilePath+".jsonld", json)
  }
}
