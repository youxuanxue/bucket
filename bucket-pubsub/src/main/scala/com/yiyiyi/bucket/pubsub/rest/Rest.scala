package com.yiyiyi.bucket.pubsub.rest

import akka.actor.ActorSystem
import akka.cluster.sharding.ClusterSharding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.yiyiyi.bucket.base
import com.yiyiyi.bucket.pubsub.PubsubBoard
import com.yiyiyi.bucket.pubsub.entity.{ PlayerEntity, RoomEntity }
import org.slf4j.{ Logger, LoggerFactory }

import scala.concurrent.ExecutionContextExecutor

/**
 * @author xuejiao
 */
trait Rest extends Directives {
  implicit val system: ActorSystem
  implicit val timeout: Timeout
  implicit val materializer: ActorMaterializer
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val log: Logger = LoggerFactory.getLogger(getClass)

  protected def pubsubBoard = PubsubBoard(system)
  protected def roomSharding = ClusterSharding(system).shardRegion(RoomEntity.typeName)
  protected def playerSharding = ClusterSharding(system).shardRegion(PlayerEntity.typeName)

  private val charset: HttpCharset = HttpCharset.custom("utf-8")
  private val mediaTYpe: MediaType.WithOpenCharset = MediaType.applicationWithOpenCharset("json")
  private val JsonContentType = ContentType.WithCharset(mediaTYpe, charset)
  private val HtmlContentType = ContentTypes.`text/html(UTF-8)`

  def htmlResponse(resp: String): HttpResponse = {
    HttpResponse(
      StatusCodes.OK,
      entity = HttpEntity(HtmlContentType, resp)
    )
  }

  def jsonResponse(resp: Any): HttpResponse = {
    HttpResponse(
      StatusCodes.OK,
      entity = HttpEntity(JsonContentType, base.objectMapper.writeValueAsString(resp))
    )
  }
}

