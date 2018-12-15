package com.github.oddodaoddo.sheffieldapp.datareader

import com.github.oddodaoddo.sheffieldapp.datastructures._
import java.io._
import java.nio.file.{Files, Paths}

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

class ElasticRawDataLoader(pathsToLoad:Array[String]) {
  private val esclient = new ElasticSearchWriter("localhost", 9300)

  pathsToLoad.foreach(path => loadLOLACoreExperiment(path))

  // get a list of files from a directory
  // FIXME: for now we assume our data is in LOLACore format
  // FIXME: need more thought on reading in non LOLACore formatted data sources!
  /*private def getFilesFromDir(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }*/

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
  def loadLOLACoreExperiment(path:String) = {
    val sanitizedPath = if (path.endsWith("/")) path else path+"/"
    // get author info
    val studyKw = List("")
    
    val indexKw = List("filename",
      "description",
      "protocol",
      "celltype",
      "species",
      "tissue",
      "antibody",
      "treatment")

    val study:Study = new LOLACoreCollectionFile(sanitizedPath + "collection.txt", studyKw, true).study
    // get study and experiment info
    val indexFile = new LOLACoreIndexFile(sanitizedPath + "index.txt", indexKw, true)
    // get list of bed files to process
    println("======================================")
    println(s"Processing ${path}")
    indexFile.fileList.
      foreach(bedFileName => {
        val absbedFilePath = sanitizedPath + "regions/" + bedFileName
        val bedFile = new LocalDiskFile(absbedFilePath)
        //println(s"Processing ${absbedFilePath}")
        val anns: List[Annotation] = bedFile.lines.map(line => 
          line.splits.map(s => {
            // FIXME: here we are making assumptions about positioning of things in a bed file!
            val e:Experiment = indexFile.experiments.get(bedFileName)
            Annotation(Segment(s(0).slice(3, s(0).size), s(1).toInt, s(2).toInt), {
              if (s.size > 3) s(3) else ""}, "s{LOLACore::$e.name}", e, study)})).flatten
        esclient.elasticWrite(anns)})
      println(s"Processed ${indexFile.fileList.size} bed files")
      //println(s"Processed ${lines} annotations.")
  }
}

class ElasticSegmentationLoader(path:String, probe:Boolean) {
}
