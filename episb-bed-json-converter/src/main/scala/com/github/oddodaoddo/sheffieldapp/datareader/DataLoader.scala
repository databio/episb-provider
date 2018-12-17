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
    val studyKw = List("collector", "data", "source", "description")
    
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
    val indexFile = new LOLACoreIndexFile(sanitizedPath + "index.txt", indexKw, false)
    // get list of bed files to process
    println("======================================")
    println(s"Processing ${path}")
    indexFile.fileList.
      foreach(bedFileName => {
        println(s"Processing ${bedFileName}")
        val absbedFilePath = sanitizedPath + "regions/" + bedFileName
        val bedFile = new LocalDiskFile(absbedFilePath)
        //println(s"Processing ${absbedFilePath}")
        val anns: List[Annotation] = bedFile.lines.map(line => 
          line.splits.map(s => {
            // FIXME: here we are making assumptions about positioning of things in a bed file!
            // FIXME: because we are loading data from LOLACore, for now we are assuming
            // that the segmentation is unknown, so "sptag" property of Annotation is simply
            // the name of the experiment
            val e:Experiment = indexFile.experiments.get(bedFileName)
            Annotation(Segment(s(0).slice(3, s(0).size), s(1).toInt, s(2).toInt), {
              if (s.size > 3) s(3) else ""}, "s{LOLACore::$e.name}", e, study)})).flatten
        esclient.elasticWrite(anns, "_annotations", "annotation")})
      println(s"Processed ${indexFile.fileList.size} bed files")
      //println(s"Processed ${lines} annotations.")
  }
}

// load up a segmentation
// load up annotations if they exist and tag them back to the segmentation
// handles multiple expriments in a single file tied to a single segmentation
object SegmentationLoader {

  private val esclient = new ElasticSearchWriter("localhost", 9300)

  // assumes non-headered file, columns 1, 2 and 3 are segment notation
  // anns is a list of positions to use as columns for annotations
  def processFile(name:String, path:String, anns:Int*) = {
    val f:LocalDiskFile = new LocalDiskFile(path)
    // get segmentation
    val segmentation = new Segmentation(name,f.lines.map(_.splits.map(ln => {
      val chr = ln(0).slice(3, ln(0).size)
      val segStart = ln(1).toInt
      val segEnd = ln(2).toInt
      new Segment(chr,segStart,segEnd)})).flatten)
    // now get all the annotations
    val emptyExp = new Experiment("","","","","","","","")
    val emptyStudy = new Study(new Author("","",""),"","","")
    val annotations:List[Annotation] = (f.lines.map(_.splits.map(ln => {
      val chr = ln(0).slice(3, ln(0).size)
      val segStart = ln(1).toInt
      val segEnd = ln(2).toInt
       val s = new Segment(chr,segStart,segEnd)
      anns.map(ann => {
        val aval = ln(ann)
        new Annotation(s, aval, name, emptyExp, emptyStudy)}).toList})).flatten).flatten
    // now that we have the segmentation and the annotations, it is time to write them to elastic
    esclient.elasticWrite(List(segmentation), "_segmentations", name)
    esclient.elasticWrite(annotations, "_annotations", "annotation")
  }

  // assume headered file with kw listing all the keywords we need
  // assumes first 3 positions are always chr, start, end
  def processFile(path:String, kw:List[String]) = {
    val f:HeaderedFile = new HeaderedFile(path, kw, true) with DiskFile
  }

/*  private val (segmentation,sptag):(Segmentation,String) = loadSegmentation
  private val annotations:List[List[Annotation]] = loadAnnotations

  // write the segmentation into elastic
  esclient.elasticWrite(List(segmentation), "_segmentations", name)
  // write all the possible annotations (if any) to elastic
  annotations.foreach(esclient.elasticWrite(_, "_annotations", "annotation"))

  // get the segment list from a file and create a segmentation from it
  private def loadSegmentation:(Segmentation,String) = {
    // we are assuming headers exist and 
  }

  private def loadAnnotations:List[List[Annotation]] = {}*/
}
