package com.yiyiyi.bucket.pubsub.model

import com.yiyiyi.bucket.base.model.Card

/**
 * @author xuejiao
 */

object RoomMessage {
  val unknown = "unknown"
  val fullRoom = "fullRoom"
  val join = "join"
  val ready = "ready"
  val leave = "leave"
  val dealingCard = "dealingCard"
  val dealCard = "dealCard"
  val compete = "compete"
  val playing = "playing"
  val play = "play"
  val played = "played"
  val checkSuccess = "checkSuccess"
  val checkFail = "checkFail"
  val hint = "hint"

}

final case class RoomEvent(
  var message: String = RoomMessage.unknown,
  var playerId: Long = 0L,
  var cards: List[Card] = List(),
  var competeBoost: Int = 1, // 抢牌翻倍倍率,
  var hints: List[List[Card]] = List()
)

