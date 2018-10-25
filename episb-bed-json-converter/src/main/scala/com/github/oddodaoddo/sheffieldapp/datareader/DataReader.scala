package com.github.oddodaoddo.sheffieldapp.datareader

import scala.io.Source

trait DataReader[T] extends java.io.Serializable {
  def read(path:String): Iterator[T]
}

trait LocalFileReader extends DataReader[String] {
  def read(path:String):Iterator[String] = Source.fromFile(path).getLines
}

trait BedFileReader {
  def processBedLine(bedLine:String):Option[(String,Int,Int,Option[String])] = {
    // decide if , or \t is the separator
    val s:Array[String] =
      if (bedLine.indexOf(",") != -1)
        bedLine.split(",")
      else if (bedLine.indexOf("\t") != -1)
        bedLine.split("\t")
      else new Array[String](0)
    if (s.size == 3)
      Some((s(0).trim,s(1).trim.toInt,s(2).trim.toInt,None))
    else if (s.size == 4)
      Some((s(0).trim,s(1).trim.toInt,s(2).trim.toInt,Some(s(3))))
    else
      None
  }
}

trait LocalBedFileReader extends BedFileReader with LocalFileReader {
  def readFile(path:String):Iterator[Option[(String,Int,Int,Option[String])]] = {
    read(path).map(ln => processBedLine(ln))
  }
}

abstract class IndexFile {
}

class LocalIndexFile extends IndexFile with LocalFileReader {
}

abstract class CollectionFile {
}

class LocalCollectionFile extends CollectionFile with LocalFileReader {
}

/*class SafeS3Reader extends DataReader[String] with java.io.Serializable {
  def read(path:String): Iterator[String] =
    Source.fromInputStream(S3Utility.s3Client.getObject(S3Utility.getS3ReadBucket, path).getObjectContent: InputStream).getLines
}*/
