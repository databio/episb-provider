package com.github.oddodaoddo.sheffieldapp.util

import com.github.oddodaoddo.sheffieldapp.datastructures._

import com.typesafe.scalalogging.LazyLogging
//import org.json4s._
//import org.json4s.native.JsonMethods._
//import org.json4s.JsonDSL._

class SegmentMatcher(sm:List[Segment]) extends LazyLogging {

  // convert compressed segmentation into a hash table
  // indexed by "chr" pointing to a tuple (segStart,segEnd)
  // sorted by segStart
  //logger.debug(s"(SegmentSearcher)::init: sm=${sm}")
  private val tpls:List[(String,Segment)] = sm.map(s => (s.segChr, s))

  //logger.debug(s"tpls=${tpls}")
  // by here we have a sorted List of tripples (chr,start,end) by chr
  // do a non-recursive re-ordering of the List[(String,Int,Int) into a
  // Map[String,List(Int,Int)] where the hash table is indexed by "chr"
  // this should speed up lookups by dividing segmentation into a set of buckets
  // within the bucket we can do binary search on "segStart" and "segEnd" fields
  // after we sort first by segStart and then segEnd
  private val emptyMap = Map.empty[String,List[Segment]].withDefaultValue(List.empty[Segment])
  private val chrBucketsUnsorted:Map[String,List[Segment]] = tpls.
    foldLeft(emptyMap)((y,x) => { if (!y.contains(x._1)) y ++ Map(x._1->List(x._2)) else y ++ Map(x._1 -> (y(x._1):+x._2))})
  // sort the Lists pointed to by "chr" according to "segStart" and then "segEnd"
  private val chrBuckets:Map[String,List[Segment]] = chrBucketsUnsorted.keys.map(k => 
    (k -> chrBucketsUnsorted(k).sortWith((x,y) => 
      (x.segChr < y.segChr) && (x.segStart<y.segStart) && (x.segEnd<y.segEnd)))).toMap

  //logger.debug(s"chrBuckets]${chrBuckets}")

  // take a segment and match it against a segmentation
  // return segment ID or None
  def exactMatch(s:Segment):Option[String] = {
    logger.info(s"(SegmentMatcher::exactMatch): Looking for segment ${s}")
    val result = if (chrBuckets.contains(s.segChr)) {
      logger.info(s"Found bucket belonging to chr=${s.segChr}")
      // found the chr of the segment
      logger.info(s"bucket=${chrBuckets(s.segChr)}")
      //val where = chrBuckets(s.segChr).indexWhere((x)=>(x._1,x._2)==(s.segStart,s.segEnd))
      val where = chrBuckets(s.segChr).indexOf(s)
      if (where == -1) None
      else Some((chrBuckets(s.segChr)(where)).segID)
    } else
        None
    logger.info(s"(SegmentMatcher::ExactMatch): ${result}")
    result
  }

  def exactMatchNoID(s:Segment):Option[String] = {
    logger.info(s"(SegmentMatcher::exactMatchNoID): Looking for segment ${s}")
    val result = if (chrBuckets.contains(s.segChr)) {
      logger.info(s"Found bucket belonging to chr=${s.segChr}")
      // found the chr of the segment
      logger.info(s"bucket=${chrBuckets(s.segChr)}")
      //val where = chrBuckets(s.segChr).indexWhere((x)=>(x._1,x._2)==(s.segStart,s.segEnd))
      val where = chrBuckets(s.segChr).indexWhere(t => 
        (t.segChr==s.segChr && t.segStart==s.segStart && t.segEnd==s.segEnd))
      if (where == -1) None
      else Some((chrBuckets(s.segChr)(where)).segID)
    } else
        None
    logger.info(s"(SegmentMatcher::ExactMatch): ${result}")
    result
  }

  // return all segments for a given segment start/end range
  def filterBySegmentRange(segStart:Int, segEnd:Int):List[Segment] = {
    tpls.filter(x => ((x._2.segStart>=segStart && x._2.segEnd<=segEnd) && 
      (x._2.segStart>=segStart && x._2.segEnd<=segEnd))).map(_._2)
  }
}
