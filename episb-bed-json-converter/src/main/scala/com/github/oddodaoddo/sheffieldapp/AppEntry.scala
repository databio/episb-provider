package com.github.oddodaoddo.sheffieldapp

import com.github.oddodaoddo.sheffieldapp.datareader._
import com.github.oddodaoddo.sheffieldapp.datastructures._
import org.rogach.scallop._

object ProcessRawLOLAData {
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

object ProcessSegmentationNonHeadered {
  class Conf(arguments:Seq[String]) extends ScallopConf(arguments) {
    //val probe = opt[Boolean]()
    val segname = opt[String](required=true)
    val expname = opt[String](required=true)
    val path = opt[String](required=true)
    val writer = opt[String](required=true)
    verify()
  }

  def main(args:Array[String]): Unit = {
    val conf = new Conf(args)
    val writer = conf.writer()
    val ww:JSONWriter = if (writer == "elastic")
      new ElasticSearchWriter("segmentation", conf.segname())
    else
      new LocalFileWriter(writer)

    new SegmentationLoader(
      conf.segname(),
      conf.expname(),
      new LocalDiskFile(conf.path()),
      ww)
  }
}
