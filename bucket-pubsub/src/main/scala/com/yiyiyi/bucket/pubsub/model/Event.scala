package com.yiyiyi.bucket.pubsub.model

import com.yiyiyi.bucket.pubsub.model.RoomIncomingMessage.RoomIncomingMessage
import com.yiyiyi.bucket.pubsub.model.RoomOutgoingMessage.RoomOutgoingMessage

/**
 * @author xuejiao
 */
final case class RoomIncomingEvent(
  var message: RoomIncomingMessage = RoomIncomingMessage.unknown
)

object RoomIncomingMessage extends Enumeration {
  type RoomIncomingMessage = Value
  val unknown, ready, leave = Value
}

final case class RoomOutgoingEvent(
  var playerId: Long = 0L,
  var message: RoomOutgoingMessage = RoomOutgoingMessage.unknown
)

object RoomOutgoingMessage extends Enumeration {
  type RoomOutgoingMessage = Value
  val unknown, fullRoom, join, leave, ready, dealCard = Value

}