package com.github.oddodaoddo.sheffieldapp.datareader

import java.nio.file.{Files, Paths}
import java.io.IOException

import com.typesafe.scalalogging.LazyLogging

import com.github.oddodaoddo.sheffieldapp.datastructures.{Author, Experiment, Study}

import scala.io.Source

// represents a line from a file
// if it is a header line, the headerline passed in has to be true
// if it is, other information becomes available
case class Line(ln:String, kw:List[String], val headerline:Boolean) extends LazyLogging {

  private def delimeters = List("\t", ",", " ")
  // try to split a line in a file, based on a few delimeters
  // we are converting to a List[String] below because it allows for a clean getOrElse later on in Headerline
  val splits:Option[List[String]] =
    delimeters.map(d => if (ln.indexOf(d) != -1) Some(ln.split(d).toList) else None).flatten.headOption

  //logger.info(s"ln=${ln}, Line.splits=${splits}")

  // get column mappings from a header line
  // column mappings are a hash table mapping a
  // (column name -> position in line)
  // lowercase column names only
  // the below operation is safe on an empty list,
  // if a list is empty, the map will be empty as well, and it will be correctly typed

  // FIXME: Not sure I like the typecast, can we see if we can use the Option as it was indended to be used?
  private val columnMappings:Map[String,Int] = 
    if (headerline)
      splits.getOrElse(List.empty[String]).map(x => x.toLowerCase).zipWithIndex.toMap
    else
      Map.empty[String,Int]

  // log things
  //logger.info(s"(class HeaderLine): columnMappings=${columnMappings}")

  // convenience methods for columnMappings
  def apply(k:String) = columnMappings(k)
  def contains(k:String):Boolean = columnMappings.contains(k)
  def size:Int = columnMappings.size
  def keys:List[String] = columnMappings.keys.toList

  // see if the header we read from the file matches the header we expected
  // FIXME: see above FIXME, how would this change to match that?
  def kwMatch:Boolean = kw.map(k => columnMappings.contains(k)).foldLeft(true)(_ && _)
}

trait FileReader extends java.io.Serializable with LazyLogging {

  val path:String
  
  def read:Option[Iterator[String]]

  def fileExists:Boolean
  val size:Int

  // we only deal in lines and headers, we do not expose contents itself
  // will be either a list of lines or an empty list

  // read in lines from a file that could be local or on the net (like a URL or on S3, e.g.)
  // to save memory, we are making this a one-time "def" instead of a val
  // we don't want to keep the "raw" string lines in memory, esp. not for large multi Gb files
  protected def contents:Option[Vector[String]] = read.map(x => x.toVector)

  //logger.info(s"(FileReader)contents=${contents}")

  // for now ALL lines (including possible header line are in below list
  val lines:Vector[Line]

  // convenience method
  lazy val isEmpty:Boolean = lines == None

  // log things
  //logger.info(s"(trait FileReader): path=${path}, size=${size})")
}

trait DiskFile extends FileReader {
  def fileExists:Boolean = Files.exists(Paths.get(path))
  def read:Option[Iterator[String]] = if (fileExists) Some(Source.fromFile(path).getLines) else None
}

// headered: indicates whether a file is assumed to have a header line
// kw: is a list of keywords in the header line to use as column names
// strictMatch: if true, it will fail if header line column names do NOT match kw
class LocalDiskFile(val path:String, val headered:Boolean, val kw:List[String], val strictMatch:Boolean) extends DiskFile {
  override lazy val size:Int = lines.size

  // die right here if we are constructing this class with contradictory arguments
  if ( (!headered && !kw.isEmpty) || (!headered && strictMatch) )
    throw new Exception("(LocalDiskFile): contradicting arguments.")

  // die if a file does not exist
  if (!fileExists)
    throw new Exception(s"File with name ${path} does not exist.")

  // for efficiency, we are 
  override val lines:Vector[Line] = {
    val c = contents
    // did we read any lines and do we have more than one line?
    if (c.isDefined && c.get.size > 1) {
      // the header line below will be treated as just another line if headered is set to false
      val header = Line(c.get.head,kw,headered)
      if ((strictMatch && header.kwMatch) || (!strictMatch))
        header +: c.get.tail.map(Line(_,kw,false))
      else
        // not really the best FP solution but we do want to quit the program
        // if keywords do not match header
        throw new Exception(s"Header line ${c.get.head} does not match keywords ${kw}")
    } else
        Vector.empty[Line]
  }
}

// representation of an index.txt file from LOLACore, local to the file system
class LOLACoreIndexFile(path:String, kw:List[String], kwMatch:Boolean) 
    extends LocalDiskFile(path, true, kw, kwMatch) with DiskFile {
  // get all files listed in index.txt
  // assumes "filename" column
  // assumes header exists
  val fileList:Vector[String] = {
    val header = lines.head
    lines.tail.
    filter(ln => header.contains("filename") && ln.splits.isDefined && 
      (ln.splits.get.size == (header.size+1) || header("filename") <= ln.splits.get.size)).
    map(_.splits.get(header("filename")))
  }

  //logger.info(s"(class LOLACoreIndexFile): fileList=${fileList}")

  // create all the Experiment objects from an index file
  // they are indexed by filename (usually a bed file filename)
  val experiments:Map[String,Experiment] = {
    // create an Experiment object based on index.txt contents, with variable column structure
    // FIXME: de-couple Experiment creation from hard-coded list of fields
    def populateExperiment(ln:List[String], h:Line,expname:String):Experiment = {
      val kwval:Map[String,String] = kw.map(k =>
        if (h.contains(k)) (k -> ln((h(k)))) else (k -> "")).toMap
      Experiment(expname,
        kwval("protocol"),
        kwval("celltype"),
        kwval("species"),
        kwval("tissue"),
        kwval("antibody"),
        kwval("treatment"),
        kwval("description"))
    }

    // assumes header exists and is the first line
    val h = lines.head
    lines.filter(ln => ln.splits.isDefined && (ln.splits.get.size != (h.size+1) || ln.splits.size <= h("filename"))).
      map(ln => ln.splits.get(h("filename")) -> populateExperiment(ln.splits.get,h,ln.splits.get(h("filename")))).toMap
  }
}

// representation of a collection.txt file from LOLACore, local to the file system
class LOLACoreCollectionFile(path:String, kw:List[String], kwMatch:Boolean) 
    extends LocalDiskFile(path, true, kw, kwMatch) with DiskFile {
  // use only the first line of the collection file after the header, ignore the rest
  val study:Study = if (!lines.isEmpty && lines.size > 1) {
    val ln = lines(1).splits.get
    val header = lines.head
    val kwval:Map[String,String] = kw.map(k => 
      if (header.contains(k)) (k -> ln.apply(header(k))) else (k -> "")).toMap

    //logger.info(s"(LOLACoreCollectionFile):kwval=${kwval}")

    if (kwval.isEmpty)
      Study(Author("Default", "Author", "info@episb.org"),"","","")
    else {
      // get Author object done first
      val authorPart: Array[String] = if (kwval.contains("collector")) kwval("collector").split(" ") else Array()
      val author: Author =
      if (authorPart.size == 2)
        Author(authorPart(0), authorPart(1), email = "")
      else
        Author(authorPart(0), "", "")
      Study(author, kwval("source"), kwval("description"), kwval("date"))
    }
  } else
      Study(Author("Default", "Author", "info@episb.org"),"","","")
}

/*class SafeS3Reader extends DataReader[String] with java.io.Serializable {
  def read(path:String): Iterator[String] =
    Source.fromInputStream(S3Utility.s3Client.getObject(S3Utility.getS3ReadBucket, path).getObjectContent: InputStream).getLines
}*/
