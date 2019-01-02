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
            Annotation("LOLACore::${randomUUID}", {
              if (s.size > 3) s(3) else ""}, e, study)})).flatten
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
  writer:JSONWriter,
  skipheader:Boolean) {

  // assumes non-headered file, columns 1, 2 and 3 are segment notation
  // annCols is a list of positions to use as columns for annotations
  //private val f:LocalDiskFile = new LocalDiskFile(path)
  // get segmentation
  val lns = if (skipheader) reader.lines.tail else reader.lines
  private val segmentation = new Segmentation(segName,lns.map(_.splits.map(ln => {
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
  val writerRes = writer.write(List(segmentation))
  writerRes match {
    case Left(msg) => println(msg)
    case Right(bool) => println("write successful")
  }
}

// assumes we know the segmentation we want to use
// we can get the whole segmentation back and use the segments for this occasion
class AnnotationLoader(segName:String, expName:String, reader:FileReader, writer:JSONWriter, col:Int) {
  /* algorithm:
   *   open file
   *   for each line
   *     get chr/start/end
   *     elastic_api_server->match(chr/start/end)?
   *       yes? create annotations linking back to segmentation_provider::segmentID
   *       no? ???
   */
  implicit val formats = DefaultFormats

  // invoke REST API point here
  // FIXME: no timeout checking, no futures, no error checking
  val url = s"http://localhost:8080/episb-rest-server/segmentation/get/ByNameWithSegments/${segName}"
  val json = Source.fromURL(url).mkString

  // we get back a segmentation in json or JsonError object
  // FIXME: Make sure we react accordingly if it is JsonError indeed
  val segmentation:Either[String,Segmentation] = {
    val j = parse(json)
    try {
      Right((j \\ "_source").extract[Segmentation])
    } catch {
      case e:Exception => Left(e.getMessage)
    }
  }

  segmentation match {
    case Right(s) => {
      // we are in business
      // we have successfully converted json into a Segmentation class
      // instance of which is "s"
      // following function traverses the list of segments for a match
      // FIXME: figure out a faster way to search!!
      
      val anns:List[Annotation] = reader.lines.map(_.splits.map(ln => {
        val chr = ln(0).slice(3, ln(0).size)
        val segStart = ln(1).toInt
        val segEnd = ln(2).toInt
        val annVal = if (ln.size >= col) ln(col) else -1

        val emptyExp:Experiment = new Experiment(expName, "", "", "", "", "", "", "")
        val emptyStudy:Study = new Study(new Author("","",""),"","","")

        val sp:Option[String] = segmentLookup(new Segment("",chr,segStart,segEnd))
        if (sp.isDefined)
          // found a segment matching an annotation   
          new Annotation(sp.get,annVal.toString,emptyExp,emptyStudy)
        else
          // FIXME: decide what to do if segment cannot be found
          new Annotation("unknown segment ID","",emptyExp,emptyStudy)
        // now we need to turn this list into a searchable data structure
        // and search it for a particular segment chr/start/end and get resulting segment ID
        // this segment ID is in form <segmentation>::uuid
        // now read annotation value at column x
        // and create an annotation object to commit to elastic (or to a file)
      })).flatten
      val writerRes = writer.write(anns)
      writerRes match {
        case Left(msg) => println(msg)
        case Right(bool) => println("write successful")
      }
    }
    case Left(msg) =>
      println(s"Could not retrieve segmentation from database. Error: ${msg}")
  }


  
}
