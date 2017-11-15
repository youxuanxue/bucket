package com.yiyiyi.bucket.pubsub.model

import com.yiyiyi.bucket.base.model.Card
import com.yiyiyi.bucket.pubsub.model.RoomMessage.RoomMessage

/**
 * @author xuejiao
 */

object RoomMessage extends Enumeration {
  type RoomMessage = Value
  val unknown = Value
  val fullRoom, join, ready, leave = Value
  val dealingCard, dealCard, compete = Value
  val playing, play, played, checkSuccess, checkFail, hint = Value

}

final case class RoomEvent(
  var message: RoomMessage = RoomMessage.unknown,
  var playerId: Long = 0L,
  var cards: List[Card] = List(),
  var competeBoost: Int = 1, // 抢牌翻倍倍率,
  var hints: List[List[Card]] = List()
)

