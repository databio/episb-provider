package com.github.oddodaoddo.sheffieldapp

import com.github.oddodaoddo.sheffieldapp.datareader._
import com.github.oddodaoddo.sheffieldapp.datastructures._
import org.rogach.scallop._

object ElasticLoadRawData {
  def main(args:Array[String]): Unit = {
    new ElasticRawDataLoader(args)
  }
}

object ElasticLoadSegmentationNonHeadered {
  class Conf(arguments:Seq[String]) extends ScallopConf(arguments) {
    //val probe = opt[Boolean]()
    val segname = opt[String](required=true)
    val path = opt[String](required=true)
    val columns = opt[List[Int]]()
    verify()
  }

  def main(args:Array[String]): Unit = {
    val conf = new Conf(args)
    SegmentationLoader.processFile(conf.segname(),conf.path(),conf.columns().toList:_*)

    //new ElasticSegmentationLoader("",true)
  }
}
