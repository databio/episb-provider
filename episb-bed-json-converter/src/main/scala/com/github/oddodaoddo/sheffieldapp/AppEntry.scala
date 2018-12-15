package com.github.oddodaoddo.sheffieldapp

import com.github.oddodaoddo.sheffieldapp.datareader._
import com.github.oddodaoddo.sheffieldapp.datastructures._
import org.rogach.scallop._

object ElasticLoadRawData {
  def main(args:Array[String]): Unit = {
    new ElasticRawDataLoader(args)
  }
}

object ElasticLoadSegmentation {
  class Conf(arguments:Seq[String]) extends ScallopConf(arguments) {
    val probe = opt[Boolean]()
    val path = opt[String](required=true)
    verify()
  }

  def main(args:Array[String]): Unit = {
    val conf = new Conf(args)
    

    //new ElasticSegmentationLoader("",true)
  }
}
