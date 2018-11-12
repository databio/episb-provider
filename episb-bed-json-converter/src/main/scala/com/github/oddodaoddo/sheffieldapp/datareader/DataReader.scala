package com.github.oddodaoddo.sheffieldapp.datareader

import java.nio.file.{Files, Paths}

import com.github.oddodaoddo.sheffieldapp.datastructures.{Author, Experiment, Study}

import scala.io.Source

abstract class DataReader[T](path:String) extends java.io.Serializable {
  // read in lines from a file that could be local or on the net
  def read:Iterator[T]
  // try to split a line in a file, based on a few delimeters
  def splitDelimitedLine(ln:String):Array[String]
}

class LocalFileReader(path:String) extends DataReader[String](path) {
  // suck in all the lines of a text file
  def read:Iterator[String] = Source.fromFile(path).getLines

  // split a line by trying a few delimeters
  // ensure that the line has been lowercased first
  // so that all the column names end up lowercased as well
  def splitDelimitedLine(ln:String):Array[String] = {
    val lnlc = ln.toLowerCase
    val delimeters = List(",", "\t", " ")
    val sp:List[Array[String]] = delimeters.map(d => if (lnlc.indexOf(d) != -1) Some(lnlc.split(d)) else None).flatten
    if (sp.isEmpty) Array() else sp.head
  }
}

class LocalFileWithHeaderReader(sanitizedPath:String) extends LocalFileReader(sanitizedPath) {
  // see if a file can be opened
  val lns:List[String] =
    if (Files.exists(Paths.get(sanitizedPath + "index.txt")))
      read.toList
    else
      List()

  // get the column mappings for the index.txt file
  // hash table: (column name -> position in line)
  val columnMappings:Map[String,Int] =
    splitDelimitedLine(lns.head).zipWithIndex.toMap
}

// representation of an index.txt file from LOLACore, local to the file system
class LocalIndexFile(sanitizedPath:String) extends LocalFileWithHeaderReader(sanitizedPath) {
  // get all files listed in index.txt
  // assumes "filename" column
  val fileList:List[String] = lns.map(ln =>
    if (columnMappings.contains("filename")) {
      val lnsplit = splitDelimitedLine(ln)
      Some(lnsplit(columnMappings("filename")))
    } else None).flatten

  // create all the Experiment objects from an index file
  // they are indexed by filename (usually a bed file filename)
  // all column names have been lowercased in the splitting process
  def getExperimentFromLOLAIndexFile:Map[String,Experiment] = {
    // create an Experiment object based on index.txt contents, with variable column structure
    def populateExperiment(ln:Array[String]):Experiment = {
      val keywords = List("filename","treatment","celltype","species","tissue","antibody","description")
      val kwval:Map[String,String] = keywords.map(kw =>
        if (columnMappings.contains(kw)) (kw -> ln(columnMappings(kw))) else (kw -> "")).toMap
      Experiment(kwval("protocol"),
        kwval("celltype"),
        kwval("species"),
        kwval("tissue"),
        kwval("antibody"),
        kwval("treatment"),
        kwval("description"))
    }

    lns.tail.map(ln => {
      val x = splitDelimitedLine(ln)
      // WARNING: below assumes we at least have a "filename" column in index.txt
      if (!x.isEmpty)
        (x(columnMappings("filename")) -> populateExperiment(x))
      else
        ("" -> Experiment("", "", "","","","",""))
    }).toMap
  }
}

// representation of a collection.txt file from LOLACore, local to the file system
class LocalCollectionFile(sanitizedPath:String) extends LocalFileWithHeaderReader(sanitizedPath) {
  // try to get Author info from LOLA collection.txt file
  def getStudyFromLOLACollectionFile:Study = {
    val lnsplit = splitDelimitedLine(lns(1)) // split on the 2nd line in collection.txt
    val keywords = List("collector", "date", "source", "description")
    val kwval:Map[String,String] = keywords.map(kw =>
      if (columnMappings.contains(kw)) (kw -> lnsplit(columnMappings(kw))) else (kw -> "")).toMap
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
  }
}

/*class SafeS3Reader extends DataReader[String] with java.io.Serializable {
  def read(path:String): Iterator[String] =
    Source.fromInputStream(S3Utility.s3Client.getObject(S3Utility.getS3ReadBucket, path).getObjectContent: InputStream).getLines
}*/
