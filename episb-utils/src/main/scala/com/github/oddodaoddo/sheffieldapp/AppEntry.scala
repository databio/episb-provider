package com.github.oddodaoddo.sheffieldapp

import com.github.oddodaoddo.sheffieldapp.datareader._
import com.github.oddodaoddo.sheffieldapp.datastructures._

import org.rogach.scallop._

import java.io.FileWriter

import com.typesafe.scalalogging.LazyLogging

object ProcessRawLOLAData extends LazyLogging {
  
  class Conf(arguments:Seq[String]) extends ScallopConf(arguments) {
    //val probe = opt[Boolean]()
    val writer = opt[String](required=true)
    val paths = trailArg[String](required=true)
    verify()
  }

  def main(args:Array[String]): Unit = {
    val conf = new Conf(args)
    val writer = conf.writer()
    val paths = conf.paths().split(" ")
    if (writer=="elastic")
      new LOLACoreConverter(paths, new ElasticSearchWriter("annotations", "annotation"))
    else
      new LOLACoreConverter(paths, new LocalFileWriter(writer))
  }
}

// if skipheader is false, the first line of the file will be skipped
// if it is true, the first line will be assumed to be a header
object ProcessSegmentation extends LazyLogging {
  class Conf(arguments:Seq[String]) extends ScallopConf(arguments) {
    //val probe = opt[Boolean]()
    val segname = opt[String](required=true)
    val expname = opt[String](required=true)
    val path = opt[String](required=true)
    val writer = opt[String](required=true)
    val skipheader = opt[Boolean]()
    verify()
  }

  def main(args:Array[String]): Unit = {
    val conf = new Conf(args)
    val writer = conf.writer()
    val (ww, wws):(JSONWriter,JSONWriter) = if (writer == "elastic") {
      (new ElasticSearchWriter("segmentations", "segmentation"),
       new ElasticSearchWriter("regions", "region"))
    } else
      (new LocalFileWriter(writer), new LocalFileWriter(writer))

    new SegmentationLoader(
      conf.segname(),
      conf.expname(),
      conf.path(),
      ww, wws,
      if (conf.skipheader.toOption.isDefined) conf.skipheader() else false)
  }
}

// if skipheader is false, the first line of the file will be skipped
// if it is true, the first line will be assumed to be a header
// assumes commital to elastic
object DummyLoad extends LazyLogging {
  class Conf(arguments:Seq[String]) extends ScallopConf(arguments) {
    //val probe = opt[Boolean]()
    val segname = opt[String](required=true)
    val expname = opt[String](required=true)
    val readfrom = opt[String](required=true)
    val skipheader = opt[Boolean]()
    val skipsegmentation = opt[Boolean]()
    val column = opt[Int](required=true)
    verify()
  }

  def main(args:Array[String]): Unit = {
    val conf = new Conf(args)

    new DummyLoader(
      conf.segname(),
      conf.expname(),
      conf.readfrom(),
      new ElasticSearchWriter("segmentations", "segmentation"),
      new ElasticSearchWriter("regions", "region"),
      new ElasticSearchWriter("annotations", "annotation"),
      conf.column(),
      if (conf.skipheader.toOption.isDefined) conf.skipheader() else false,
      if (conf.skipsegmentation.toOption.isDefined) conf.skipsegmentation() else false)
  }
}
