package com.github.oddodaoddo.sheffieldapp

import com.github.oddodaoddo.sheffieldapp.datareader._
import com.github.oddodaoddo.sheffieldapp.datastructures._
import org.rogach.scallop._

object ElasticLoadRawData {
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

object ElasticLoadSegmentationNonHeadered {
  class Conf(arguments:Seq[String]) extends ScallopConf(arguments) {
    //val probe = opt[Boolean]()
    val segname = opt[String](required=true)
    val expname = opt[String](required=true)
    val path = opt[String](required=true)
    val writer = opt[String](required=true)
    val columns = opt[List[Int]]()
    verify()
  }

  def main(args:Array[String]): Unit = {
    val conf = new Conf(args)
    val columns:List[Int] = if (conf.columns.isDefined) 
      conf.columns()
    else
      List.empty
    val writer = conf.writer()
    val ww:JSONWriter = if (writer == "elastic")
      new ElasticSearchWriter("segmentation", conf.segname())
    else
      new LocalFileWriter(writer)

    new SegmentationLoader(
      conf.path(), 
      conf.segname(),
      conf.expname(),
      ww,
      columns: _*) with DiskFile
  }
}
