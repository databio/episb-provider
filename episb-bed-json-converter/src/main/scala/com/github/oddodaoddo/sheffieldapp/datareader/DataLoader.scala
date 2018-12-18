package com.github.oddodaoddo.sheffieldapp.datareader

import com.github.oddodaoddo.sheffieldapp.datastructures._

import java.io._
import java.nio.file.{Files, Paths}
import java.util.UUID.randomUUID

import scala.io.Source

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

class LOLACoreConverter(pathsToLoad:Array[String], writer:JSONWriter) {

  pathsToLoad.foreach(path => loadLOLACoreExperiment(path))

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
    //println("======================================")
    //println(s"Processing ${path}")
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
            Annotation(Segment(s"LOLACore::${randomUUID}",s(0).slice(3, s(0).size), s(1).toInt, s(2).toInt), {
              if (s.size > 3) s(3) else ""}, s"LOLACore::${e.name}", e, study)})).flatten
        writer.write(anns)})
      //println(s"Processed ${indexFile.fileList.size} bed files")
      //println(s"Processed ${lines} annotations.")
  }
}

// load up a segmentation
// load up annotations if they exist and tag them back to the segmentation
// only works on local files for now
class SegmentationLoader(
  segName:String,
  expName:String,
  reader: FileReader,
  writer:JSONWriter) {

  // assumes non-headered file, columns 1, 2 and 3 are segment notation
  // annCols is a list of positions to use as columns for annotations
  //private val f:LocalDiskFile = new LocalDiskFile(path)
  // get segmentation
  private val segmentation = new Segmentation(segName,reader.lines.map(_.splits.map(ln => {
    val chr = ln(0).slice(3, ln(0).size)
    val segStart = ln(1).toInt
    val segEnd = ln(2).toInt
    // here we assign a random UUID to the segment so it can be uniquely identified
    new Segment(s"${segName}::${randomUUID.toString}",chr,segStart,segEnd)})).flatten)
    // now get all the annotations
    //val emptyExp = new Experiment("","","","","","","","")
    //val emptyStudy = new Study(new Author("","",""),"","","")
    /*val annotations:List[Annotation] = (f.lines.map(_.splits.map(ln => {
      val chr = ln(0).slice(3, ln(0).size)
      val segStart = ln(1).toInt
      val segEnd = ln(2).toInt
       val s = new Segment(chr,segStart,segEnd)
      annCols.map(ann => {
        val aval = ln(ann)
        new Annotation(s, aval, name, emptyExp, emptyStudy)}).toList})).flatten).flatten*/
    // now that we have the segmentation and the annotations, it is time to write them to elastic
  writer.write(List(segmentation))//, "segmentations", name)
    //writer.write(annotations)//, "annotations", "annotation")
}

// assumes we know the segmentation we want to use
// we can get the whole segmentation back and use the segments for this occasion
class AnnotationLoader(reader:FileReader, writer:JSONWriter, segName:String, columns:Int*) {
  /* algorithm:
   *   open file
   *   for each line
   *     get chr/start/end
   *     elastic_api_server->match(chr/start/end)?
   *       yes? create annotations linking back to segmentation_provider::segmentID
   *       no? ???
   */

  reader.lines.map(_.splits.map(ln => {
    val chr = ln(0).slice(3, ln(0).size)
    val segStart = ln(1).toInt
    val segEnd = ln(2).toInt

    // invoke REST API point here
    // FIXME: no timeout checking, no futures, no error checking
    val url = s"http://localhost:8080/segmentation/get/ByNameWithSegments/${segName}"
    val json = Source.fromURL(url).mkString
    // we get back a list of actual segments and their IDs
    // now we need to turn this list into a searchable data structure
    // and search it for a particular segment chr/start/end and get resulting segment ID
    // this segment ID is in form <segmentation>::uuid
    // now read annotation value at column x
    // and create an annotation object to commit to elastic (or to a file)
  }))
}
