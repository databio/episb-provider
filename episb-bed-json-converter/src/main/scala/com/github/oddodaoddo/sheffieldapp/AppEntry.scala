package com.github.oddodaoddo.sheffieldapp

import com.github.oddodaoddo.sheffieldapp.datareader.StudyElasticLoaderConverter
import com.github.oddodaoddo.sheffieldapp.datastructures._

object AppEntry {
  def main(args:Array[String]): Unit = {
    new StudyElasticLoaderConverter(args(0)).loadData
  }
}
