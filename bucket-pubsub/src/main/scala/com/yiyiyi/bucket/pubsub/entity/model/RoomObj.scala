package com.yiyiyi.bucket.pubsub.entity.model

import com.yiyiyi.bucket.base.model.{ CardType, RoomStatus }
import com.yiyiyi.bucket.base.model.CardType.CardType
import com.yiyiyi.bucket.base.model.RoomStatus.RoomStatus

/**
 * @author xuejiao
 */
final case class RoomObj(
    id: Long = 0L,
    ownerId: Long = 0L,
    cardType: CardType = CardType.unknown,
    var status: RoomStatus = RoomStatus.off,
    var players: Map[Long, Long] = Map() // playerId, timestamp/index
) {

  def isFull: Boolean = {
    isAvailable &&
      CardType.playerNumLimit(cardType) > 0 &&
      players.size == CardType.playerNumLimit(cardType)
  }

  def isAvailable: Boolean = {
    status == RoomStatus.on
  }

}

