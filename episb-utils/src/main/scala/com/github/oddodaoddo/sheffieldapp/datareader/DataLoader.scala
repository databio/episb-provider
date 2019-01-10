package com.github.oddodaoddo.sheffieldapp.datareader

import com.github.oddodaoddo.sheffieldapp.datastructures._
import com.github.oddodaoddo.sheffieldapp.util._

import java.io._
import java.nio.file.{Files, Paths}
import java.util.UUID.randomUUID

import scala.io.Source

import com.typesafe.scalalogging.LazyLogging
import com.typesafe.config._

import scalaj.http._

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

class LOLACoreConverter(pathsToLoad:Array[String], writer:JSONWriter) extends LazyLogging {

  val constructBaseUrl:String = {
    private val conf = ConfigFactory.load()
    val providerUrl = conf.getString("episb-utils.provider-url")
    val providerUrlBase = conf.getString("episb-utils.provider-url-base")
    val providerPort = conf.getString("episb-utils.episb-provider-port")
    val finalUrl = "http://"+providerUrl+":"+providerPort+"/"+providerUrlBase
    if (finalUrl.endsWith("/")) finalUrl.init else finalUrl
  }

  logger.info(s"(LOLACoreConverter): pathsToLoad=${pathsToLoad}")

  pathsToLoad.foreach(path => loadLOLACoreExperiment(path))

  // all bed files are assumed to live under path/regions/ subfolder
  // path points to the top folder of a LOLA core db member study
  // we expect a file named index.txt may exist, collection.txt may exist as well
  // if index.txt does not exist, we will attempt and read the regions/ subfolder as it is
  def loadLOLACoreExperiment(path:String) = {
    val sanitizedPath = if (path.endsWith("/")) path else path+"/"
    // get author info
    val studyKw = List("collector", "date", "source", "description")
    
    val indexKw = List("filename",
      "description",
      "protocol",
      "celltype",
      "species",
      "tissue",
      "antibody",
      "treatment")

    val study:Study = new LOLACoreCollectionFile(sanitizedPath + "collection.txt", studyKw, false).study
    // get study and experiment info
    val indexFile = new LOLACoreIndexFile(sanitizedPath + "index.txt", indexKw, false)
    // get list of bed files to process
    //println("======================================")
    //println(s"Processing ${path}")
    indexFile.fileList.
      foreach(bedFileName => {
        logger.info(s"(LOLACoreConverter::loadLOLACoreExperiment): Processing ${bedFileName}")
        val absbedFilePath = sanitizedPath + "regions/" + bedFileName
        logger.info(s"(LOLACoreConverter::loadLOLACoreExperiment): absolute bed filename = ${absbedFilePath}")
        val bedFile = new LocalDiskFile(absbedFilePath)
        //println(s"Processing ${absbedFilePath}")
        val anns: List[Annotation] = bedFile.lines.map(line => 
          line.splits.map(s => {
            // FIXME: here we are making assumptions about positioning of things in a bed file!
            // FIXME: because we are loading data from LOLACore, for now we are assuming
            // that the segmentation is unknown, so "sptag" property of Annotation is simply
            // the name of the experiment
            val e:Experiment = indexFile.experiments.get(bedFileName)
            Annotation(s"LOLACore::${randomUUID}", {
              if (s.size > 3) s(3) else ""}, e, study)})).flatten
        writer.write(anns) // write to wherever
        logger.info(s"(LOLACoreConverter::loadLOLACoreExperiment): Processed ${anns.size} annotations.")
      })
      logger.info(s"(LOLACoreConverter::loadLOLACoreExperiment): Processed ${indexFile.fileList.size} bed files")
  }
}

// load up a segmentation
// load up annotations if they exist and tag them back to the segmentation
// only works on local files for now
class SegmentationLoader(
  segName:String,
  expName:String,
  reader:FileReader,
  segmentationWriter:JSONWriter,
  segmentsWriter:JSONWriter,
  skipheader:Boolean) extends LazyLogging {

  // assumes non-headered file, columns 1, 2 and 3 are segment notation
  // get segmentation
  logger.info(s"(SegmentationLoader::params): segName=s{segName}, expName=s{expName}, skipheader=s{skipheader}")

  val lns = if (skipheader) reader.lines.tail else reader.lines

  private val segs:List[Segment] = lns.map(_.splits.map(ln => {
    val chr = ln(0).slice(3, ln(0).size)
    val segStart = ln(1).toInt
    val segEnd = ln(2).toInt
    // here we assign a random UUID to the segment so it can be uniquely identified
    //logger.info(s"(SegmentationLoader): creating Segment-> chr=${chr}, start=${segStart}, end=${segEnd}")
    Segment(s"${segName}::${randomUUID}",chr,segStart,segEnd)})).flatten

  private val segmentation:Segmentation = new Segmentation(segName, segs.map(_.segID))
  // write the segments first into elastic
  // now that we have the segmentation and the annotations, it is time to write them to elastic
  segmentationWriter.write(List(segmentation)) match {
    case Left(msg) => logger.info(s"(SegmentationLoader::write) unsuccessful. msg=${msg}")
    case Right(bool) => logger.info("(SegmentationLoader::write) write successful")
  }
  // do the same with the compressed segment list (for faster searching)
  segmentsWriter.write(segs) match {
    case Left(msg) => logger.info(s"(SegmentationLoader::write-segments) unsuccessful. msg=${msg}")
    case Right(bool) => logger.info("(SegmentationLoader::write-segments) write successful")
  }
}

// assumes we know the segmentation we want to use
// we can get the whole segmentation back and use the segments for this occasion
// write produce file (through "writer") that can be submitted to an API point to load in experiment
// will load said file using API point
// will also produce json design interface and update the list of segmentations in elastic (via API point)
class AnnotationLoader(segName:String,
  expName:String,
  reader:FileReader,
  writeToPath:String,
  col:Int) extends LazyLogging {

  implicit val formats = DefaultFormats

  // create the output file writer
  val writer = new FileWriter(writeToPath)

  // invoke REST API point here
  // FIXME: no timeout checking, no futures, no error checking
  val url = s"${constructBaseUrl}/segments/get/BySegmentationName/${segName}"
  // we get back a segmentation in json or JsonError object
  // FIXME: Make sure we react accordingly if it is JsonError indeed
  val elasticHits:Either[String,Hits] = {
    try {
      val j = parse(Source.fromURL(url).mkString)
      // get the actual list of hits from elasticsearch
      Right((j \ "hits").extract[Hits])
    } catch {
      case e:Exception => Left(e.getMessage)
    }
  }

  elasticHits match {
    case Right(h) => {
      // we are in business
      // we have successfully converted json into a Segmentation class
      // instance of which is "s"
      // following function traverses the list of segments for a match
      // FIXME: figure out a faster way to search!!
      val segments:List[Segment] = h.hits.map(_._source)
      val segmentSearcher:SegmentMatcher = new SegmentMatcher(segments)
      val anns:List[Annotation] = reader.lines.map(_.splits.map(ln => {
        //logger.info(s"(AnnotationLoader): ln=${ln}")
        val chr = ln(0).slice(3, ln(0).size)
        val segStart = ln(1).toInt
        val segEnd = ln(2).toInt
        val annVal = if (ln.size >= col) ln(col) else -1
        logger.info(s"(AnnotationLoader): chr=${chr},start=${segStart},end=${segEnd},annVal=${annVal}")

        val emptyExp:Experiment = new Experiment(expName, "", "", "", "", "", "", "")
        val emptyStudy:Study = new Study(new Author("","",""),"","","")

        val sp:Option[String] = segmentSearcher.exactMatchNoID(new Segment("",chr,segStart,segEnd))
        if (sp.isDefined)
          // found a segment matching an annotation   
          new Annotation(sp.get,annVal.toString,emptyExp,emptyStudy)
        else
          // FIXME: decide what to do if segment cannot be found
          new Annotation("unknown segment ID","-1",emptyExp,emptyStudy)
        // now we need to turn this list into a searchable data structure
        // and search it for a particular segment chr/start/end and get resulting segment ID
        // this segment ID is in form <segmentation>::uuid
        // now read annotation value at column
        // and create an annotation object to commit to elastic (or to a file)
      })).flatten
      // create annotation list file to load into elastic with format:
      // ann_value<tab>segmentID\n
      val outputStr:String = anns.map(ann => s"${ann.annValue}\t${ann.segmentID}").mkString("\n")
      logger.info(s"(AnnotationLoader):output=${outputStr}")
      try {
        writer.write(outputStr)
        writer.close
      } catch {
        // log error for now
        case e:Exception => {
          logger.error(s"(AnnotationLoader): failed writing into file. Err: ${e.getMessage}")
          println(s"Problem writing into file; ${e.getMessage}")
        }

      }

      // now call API point to load in experiment into elastic
      // first, prepare multi part form to post to server
      //val formContentsStart = """--a93f5485f279c0 content-disposition: form-data; name="expfile"; filename="exp.out"\n\n"""
      //val formContentsEnd = """\n--a93f5485f279c0--"""
      //val formContents = formContentsStart + output + formContentsEnd

      // now create the actual POST request
      // should be equivalent to: 
      // curl http://localhost:8080/experiments/add/preformatted/testexperiment/testsegmentation --data-binary @/tmp/multipart-message.data -X POST -i -H "Content-Type: multipart/form-data; boundary=a93f5485f279c0"
      val formUrl = s"${constructBaseUrl}/experiments/add/preformatted/${expName}/${segName}"

      val fileInByteArrayForm:Array[Byte] = outputStr.map(ch=>ch.toByte).toArray
      // now submit the form as a POST request to the REST API server
      val result = 
        Http(formUrl).
          postMulti(MultiPart("expfile", writeToPath, "text/plain", fileInByteArrayForm)).
          option(HttpOptions.connTimeout(10000)).option(HttpOptions.readTimeout(50000)).asString

      println(result.toString)

      // FIXME: for demo purposes we assume Ints for annotation values
      // no error handling on conversion right now
      // get annotation val min
      val annValMin:Int = anns.map(ann => ann.annValue.toInt).min
      val annValMax:Int = anns.map(ann => ann.annValue.toInt).max

      // and add design interface at the end
      // FIXME: we don't care if this design doc already exists
      val di = DesignInterface("episb-provider",
                               "sample segmentation provider",
                               segName,
                               expName,
                               "sample cell type",
                               "sample experiment description",
                               "value",
                               annValMin.toString,
                               annValMax.toString)

      val diUrl = s"${constructBaseUrl/segmentations/update}"
      println(Http(diUrl).postData(di.toJsonLD).header("content-type", "application/json").
        option(HttpOptions.connTimeout(10000)).option(HttpOptions.readTimeout(50000)).asString)
    }
    case Left(msg) =>
      println(s"Could not retrieve segmentation from database. Error: ${msg}")
  }
}
