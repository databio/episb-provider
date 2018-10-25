package com.github.oddodaoddo.sheffieldapp

import com.github.oddodaoddo.sheffieldapp.datareader.StudyElasticLoaderConverter
import com.github.oddodaoddo.sheffieldapp.datastructures._

object AppEntry {
  def main(args:Array[String]): Unit = {
    new StudyElasticLoaderConverter().
      loadData(path = args(0))
    //loadData("/home/maketo/dev/sheffield/ext/qumulo/LOLAweb/databases/LOLACore/hg38/encode_tfbs")
  }
}
