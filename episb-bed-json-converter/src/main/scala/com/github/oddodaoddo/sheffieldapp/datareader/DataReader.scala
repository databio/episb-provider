package com.github.oddodaoddo.sheffieldapp.datareader

import java.nio.file.{Files, Paths}
import java.io.IOException

import com.github.oddodaoddo.sheffieldapp.datastructures.{Author, Experiment, Study}

import scala.io.Source

class Line(ln:String) {

  private val delimeters = List("\t", " ", ",")
  // try to split a line in a file, based on a few delimeters
  // we are converting to a List[String] below because it allows for a clean getOrElse later on in Headerline
  val splits:Option[List[String]] =
    delimeters.map(d => if (ln.indexOf(d) != -1) Some(ln.split(d).toList) else None).flatten.headOption
}

class HeaderLine(ln:String, kw:List[String]) extends Line(ln) {
  // get column mappings from a header line
  // column mappings are a hash table mapping a
  // (column name -> position in line)
  // lowercase column names only
  // the below operation is safe on an empty list,
  // if a list is empty, the map will be empty as well, and it will be correctly typed

  // FIXME: Not sure I like the typecast, can we see if we can use the Option as it was indended to be used?
  private val columnMappings:Map[String,Int] =
    splits.getOrElse(List().asInstanceOf[List[String]]).map(x => x.toLowerCase).zipWithIndex.toMap

  // convenience methods
  def apply(k:String) = columnMappings(k)
  def contains(k:String):Boolean = columnMappings.contains(k)
  def size:Int = columnMappings.size
  def keys:List[String] = columnMappings.keys.toList

  // see if the header we read from the file matches the header we expected
  // FIXME: see above FIXME, how would this change to match that?
  def kwMatch:Boolean = kw.map(k => columnMappings.contains(k)).foldLeft(true)(_ && _)
}

trait FileReader extends java.io.Serializable {

  val path:String
  
  def read:Option[Iterator[String]]

  def fileExists:Boolean
  def size:Int = if (contents != None) contents.size else 0

  // we only deal in lines and headers, we do not expose contents itself
  // will be either a list of lines or an empty list

  // read in lines from a file that could be local or on the net (like a URL or on S3, e.g.)
  protected val contents:Option[List[String]] = read.map(x => x.toList)

  val lines:List[Line] = if (contents.isDefined && contents.get.size > 0) 
    contents.get.map(new Line(_))
  else List.empty

  // convenience method
  def isEmpty:Boolean = contents == None
}

trait DiskFile extends FileReader {
  def fileExists:Boolean = Files.exists(Paths.get(path))
  def read:Option[Iterator[String]] = if (fileExists) Some(Source.fromFile(path).getLines) else None
}

class LocalDiskFile(override val path:String) extends DiskFile

// a file with a header line
abstract class HeaderedFile(override val path:String, kw:List[String], strictMatch:Boolean) extends FileReader {
  // will be either a list of lines or an empty list
  override val lines:List[Line] = if (contents.isDefined && contents.get.size > 1)
    contents.get.tail.map(new Line(_)) 
  else List.empty

  val header:Option[HeaderLine] = if (contents.isDefined && contents.get.size > 1) {
    val hl = new HeaderLine(contents.get.head, kw)
    if ((strictMatch && hl.kwMatch) || (!strictMatch))
      Some(hl)
    else
      None
  } else
      None
}

// representation of an index.txt file from LOLACore, local to the file system
class LOLACoreIndexFile(path:String, kw:List[String], kwMatch:Boolean) 
    extends HeaderedFile(path, kw, kwMatch) with DiskFile {
  // get all files listed in index.txt
  // assumes "filename" column
  val fileList:List[String] = lines.
    filter(ln => (header.isDefined && header.get.contains("filename")) &&
      ln.splits.isDefined && 
      (ln.splits.get.size == (header.get.size+1) || header.get("filename") <= ln.splits.get.size)).
    map(_.splits.get(header.get("filename")))

  // create all the Experiment objects from an index file
  // they are indexed by filename (usually a bed file filename)
  val experiments:Option[Map[String,Experiment]] = {
    // create an Experiment object based on index.txt contents, with variable column structure
    // FIXME: de-couple Experiment creation from hard-coded list of fields
    def populateExperiment(ln:List[String], h:HeaderLine,expname:String):Experiment = {
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

    if (header != None) {
      val h = header.get
      Some(lines.filter(ln => ln.splits.isDefined && (ln.splits.get.size != (h.size+1) || ln.splits.size <= h("filename"))).
        map(ln => ln.splits.get(h("filename")) -> populateExperiment(ln.splits.get,h,ln.splits.get(h("filename")))).toMap)
    } else
        None
  }
}

// representation of a collection.txt file from LOLACore, local to the file system
class LOLACoreCollectionFile(path:String, kw:List[String], kwMatch:Boolean) 
    extends HeaderedFile(path, kw, kwMatch) with DiskFile {
  // use only the first line of the collection file after the header, ignore the rest
  val study:Study = if (!lines.isEmpty && lines(0).splits.isDefined && header.isDefined) {
    val ln = lines(0).splits.get
    val h = header.get
    val kwval:Map[String,String] = kw.map(k => 
      if (h.contains(k)) (k -> ln.apply(h(k))) else (k -> "")).toMap
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

// the following class probes a text file to discover its header
class HeaderProber(path:String) extends HeaderedFile(path, List.empty, false) with DiskFile {
  def getHeaderKeywords:List[String] = if (header != None) header.get.keys else List.empty
}

/*class SafeS3Reader extends DataReader[String] with java.io.Serializable {
  def read(path:String): Iterator[String] =
    Source.fromInputStream(S3Utility.s3Client.getObject(S3Utility.getS3ReadBucket, path).getObjectContent: InputStream).getLines
}*/
