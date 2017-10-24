package com.yiyiyi.bucket.base.request

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ HttpRequest, Uri }
import akka.stream.ActorMaterializer
import org.slf4j.{ Logger, LoggerFactory }

import scala.concurrent.ExecutionContextExecutor

/**
 * @author xuejiao
 */
class BaseRequest(@transient val request: HttpRequest)(implicit system: ActorSystem) extends Cloneable {
  @transient implicit val ec: ExecutionContextExecutor = system.dispatcher
  @transient implicit val materializer = ActorMaterializer()
  @transient val log: Logger = LoggerFactory.getLogger(getClass)
  @transient var requestURL: String = request.uri.toString()
  @transient var host: String = request.uri.scheme + ":" + request.uri.authority.toString()

  @transient var queryMap: Map[String, String] = request.uri.query().toMap

  var udid: String = queryMap.getOrElse("udid", "")

  def asMap: Map[String, String] = {
    var map: Map[String, String] = queryMap
    if (udid.nonEmpty) map += ("udid" -> udid)

    map
  }

  def toUri: Uri = {
    Uri(requestURL).withQuery(Query(asMap))
  }

  override def toString: String = {
    toUri.toString
  }

}
