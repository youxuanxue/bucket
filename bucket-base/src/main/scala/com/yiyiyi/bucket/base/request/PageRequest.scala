package com.yiyiyi.bucket.base.request

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ HttpRequest, Uri }

class PageRequest(request: HttpRequest)(implicit system: ActorSystem) extends BaseRequest(request) {
  var start: Int = queryMap.getOrElse("start", "0").toInt
  var max: Int = queryMap.getOrElse("max", "10").toInt

  def hasPrevious: Boolean = {
    start > 0
  }

  def next: PageRequest = {
    val next: PageRequest = this.clone.asInstanceOf[PageRequest]
    next.start = start + max
    next.max = max
    next
  }

  def previous: PageRequest = {
    if (start == 0) {
      val previous: PageRequest = this.clone.asInstanceOf[PageRequest]
      previous
    }
    else if (start < max) {
      val previous: PageRequest = this.clone.asInstanceOf[PageRequest]
      previous.start = 0
      previous.max = start
      previous
    }
    else {
      val previous: PageRequest = this.clone.asInstanceOf[PageRequest]
      previous.start = start - max
      previous.max = max
      previous
    }
  }

  def first: PageRequest = {
    val first: PageRequest = this.clone.asInstanceOf[PageRequest]
    first.start = 0
    first.max = max
    first
  }

  override def toUri: Uri = {
    Uri(requestURL).withQuery(Query(asMap))
  }

  override def asMap: Map[String, String] = {
    var map: Map[String, String] = super.asMap
    map += ("start" -> start.toString)
    map += ("max" -> max.toString)
    map
  }
}