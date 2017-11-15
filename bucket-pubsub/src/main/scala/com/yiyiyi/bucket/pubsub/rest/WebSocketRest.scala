package com.yiyiyi.bucket.pubsub.rest

import akka.NotUsed
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.{ PathMatcher, Route }
import akka.stream._
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.yiyiyi.bucket.base
import com.yiyiyi.bucket.pubsub.entity.RoomEntity
import com.yiyiyi.bucket.pubsub.model.RoomEvent
/**
 * @author xuejiao
 */
trait WebSocketRest extends Rest {

  val matcher: PathMatcher[(Long, Long)] =
    "room" / "" ~ LongNumber / "" ~ LongNumber

  def wsApi: Route = pathPrefix("v1") {
    path(matcher) {
      case (roomId, playerId) =>
        get {
          handleWebSocketMessages(roomFlow(roomId, playerId))
        }
    }
  }

  private def roomFlow(roomId: Long, playerId: Long): Flow[Message, Message, Any] = {

    val sink = Flow[Message].map {
      case message: TextMessage =>
        try {
          val event = base.objectMapper.readValue(message.getStrictText, classOf[RoomEvent])
          RoomEntity.Sink(roomId, playerId, event)
        }
        catch {
          case ex: Throwable =>
            log.error(s"not an event: $message")
            RoomEntity.Sink(roomId, playerId, RoomEvent())
        }

      case x =>
        log.error(s"not textMessage: $x")
        RoomEntity.Sink(roomId, playerId, RoomEvent())
    }
      .to(Sink.actorRef[RoomEntity.Sink](roomSharding, RoomEntity.Nothing))

    val source = Source.actorRef[RoomEvent](10, OverflowStrategy.dropBuffer)
      .mapMaterializedValue { outActor =>
        roomSharding ! RoomEntity.Source(roomId, playerId, outActor)
        NotUsed
      }
      .map(outMsg => TextMessage(base.objectMapper.writeValueAsString(outMsg)))

    Flow.fromSinkAndSource(sink, source)
  }

}
