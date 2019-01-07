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

object ProcessSegmentationNonHeadered extends LazyLogging {
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
    val (ww, wwc):(JSONWriter,JSONWriter) = if (writer == "elastic") {
      (new ElasticSearchWriter("segmentations", "segmentation"),
       new ElasticSearchWriter("compressed_segmentations", "segmentation"))
    } else
      (new LocalFileWriter(writer), new LocalFileWriter(writer))

    new SegmentationLoader(
      conf.segname(),
      conf.expname(),
      new LocalDiskFile(conf.path()),
      ww, wwc,
      if (conf.skipheader.toOption.isDefined) conf.skipheader() else false)
  }
}

object ProcessAnnotationNonHeadered extends LazyLogging {
  class Conf(arguments:Seq[String]) extends ScallopConf(arguments) {
    //val probe = opt[Boolean]()
    val segname = opt[String](required=true)
    val expname = opt[String](required=true)
    val readfrom = opt[String](required=true)
    val writeto = opt[String](required=true)
    val column = opt[Int](required=true)
    verify()
  }

  def main(args:Array[String]): Unit = {
    val conf = new Conf(args)

    new AnnotationLoader(
      conf.segname(),
      conf.expname(),
      new LocalDiskFile(conf.readfrom()),
      conf.writeto(),
      conf.column())
  }
}
