package com.github.oddodaoddo.sheffieldapp.datareader

import com.github.oddodaoddo.sheffieldapp.datastructures._
import java.nio.file.{Files, Paths}
import java.io._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

trait DataLoader[T] {
  def loadData(path:String):T
}

class StudyElasticLoaderConverter extends DataLoader[Unit] with LocalBedFileReader {

  private val esclient = new ElasticSearchWriter("localhost", 9300)

  // get a list of files from a directory
  private def getFilesFromDir(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  // split a line by trying a few delimeters
  def splitDelimitedLine(ln:String):Option[Array[String]] = {
    if (ln.indexOf(",") != -1)
      Some(ln.split(","))
    else if (ln.indexOf("\t") != -1)
      Some(ln.split("\t"))
    else
      None
  }

  // try to get Author info from LOLA collection.txt file
  def getStudyFromLOLACollectionFile(sanitizedPath:String):Study = {
    if (Files.exists(Paths.get(sanitizedPath + "collection.txt"))) {
      // we must assume that the author's first and last name are space separated
      val lnsColl = read(sanitizedPath + "collection.txt").toList.tail.head
      val lns:Option[Array[String]] = splitDelimitedLine(lnsColl)
      lns match {
        case Some(x) => {
          val author:Author = {
            val names = x(0).split(" ")
            if (names.size == 2)
              Author(names(0), names(1), email = "")
            else
              Author(names(0), "", "")
          }
          val date:String = x(1)
          val manuscript:String = x(2)
          val description:String = x(3)
          Study(author,manuscript,description,date)
        }
        case None => Study(Author("Default", "Author", "info@episb.org"),"","","")
      }
    } else
      Study(Author("Default", "Author", "info@episb.org"),"","","")
  }

  def getExperimentFromLOLAIndexFile(sanitizedPath:String):Map[String,Experiment] = {
    if (Files.exists(Paths.get(sanitizedPath + "index.txt"))) {
      // we must assume that the author's first and last name are space separated
      val lnsIdx:List[String] = read(sanitizedPath + "index.txt").toList.tail
      lnsIdx.map(ln => {
        val lnsplit:Option[Array[String]] = splitDelimitedLine(ln)
        lnsplit match {
          case Some(x) => (x(0) -> Experiment(x(2), x(1), x(4))) // FIXME: assumes column ordering and number!!
          case None => ("" -> Experiment("", "", ""))
        }
      }).toMap
    } else
      Map("" -> Experiment("","",""))
  }

  // either get a list of files from index.txt or get it from regions/
  // by reading the directory
  def getListOfFilesToProcess(sanitizedPath:String):List[File] = {
    if (Files.exists(Paths.get(sanitizedPath + "index.txt")))
      read(sanitizedPath + "index.txt").toList.tail.
        map(ln => splitDelimitedLine(ln)).flatten.map(x => new File(x(0)))
    else
      getFilesFromDir(sanitizedPath+"/regions")
  }

  // map the name of the column to the position in a file line
  /*def getHeadersFromLOLAIndexFile(sanitizedPath:String):Map[String,Int] = {

  }*/

  // all bed files are assumed to live under path/regions/ subfolder
  // path points to the top folder of a LOLA core db member study
  // we expect a file named index.txt may exist, collection.txt may exist as well
  // if index.txt does not exist, we will attempt and read the regions/ subfolder as it is
  override def loadData(path: String) = {
    val sanitizedPath = if (path.endsWith("/")) path else path+"/"
    // get author info
    val study:Study = getStudyFromLOLACollectionFile(sanitizedPath)
    // get study and experiment info
    val expMap:Map[String,Experiment] = getExperimentFromLOLAIndexFile(sanitizedPath)
    // get list of bed files to process
    getListOfFilesToProcess(sanitizedPath).
      map(bedFile => {
        val absbedFilePath = sanitizedPath + "regions/" + bedFile
        println(s"Processing ${absbedFilePath}")
        val bedFileLines:List[Option[(String,Int,Int,Option[String])]] =
          if (Files.exists(Paths.get(absbedFilePath)))
            readFile(absbedFilePath).toList
          else
            List.empty[Option[(String,Int,Int,Option[String])]]
        val anns:List[Annotation] = bedFileLines.map(bfl => {
          bfl match {
            case Some(x) => {
              Annotation(Segment(x._1.slice(3, x._1.size), x._2, x._3), {
                x._4 match {
                  case Some(y) => y
                  case None => ""
                }
              },expMap(bedFile.getName),study)
            }
            case None => Annotation(Segment("0",0,0),"",Experiment("","",""),study)
          }
        }) // here we have a list of annotation objects ready to put into elastic
        // we still need to populate the Study and Experiment objects here
        // generate a valid json string from experiment
        // commit to json file on disk
        //fileWrite(bedFilePath+".jsonld", json)
        esclient.elasticWrite(anns)
      })
  }
}
