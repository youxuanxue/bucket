package com.yiyiyi.bucket.pubsub.entity.model

import com.yiyiyi.bucket.base.model.RoomStatus.RoomStatus
import com.yiyiyi.bucket.base.model.{ Card, RoomStatus }
import com.yiyiyi.bucket.pubsub.model.{ RoomEvent, RoomMessage }
import com.yiyiyi.bucket.pubsub.service.Poker

/**
 * @author xuejiao
 */
final case class RoomObj(
    id: Long,
    ownerId: Long,
    poker: Poker,
    var status: RoomStatus = RoomStatus.off,
    var players: Map[Long, PlayerObj] = Map(),
    var playingSort: List[Long] = List(),
    var playingIndex: Int = -1,
    var againstCards: List[Card] = List(),
    var competeBoost: Int = 0 // 0 表示没有人抢牌，否则在下一轮要加倍
) {

  def isFull: Boolean = {
    isAvailable &&
      poker != null &&
      poker.playerNum > 0 &&
      players.size == poker.playerNum
  }

  def isAvailable: Boolean = {
    status == RoomStatus.on
  }

  def deal(): Unit = {
    val readyEvent = RoomEvent(message = RoomMessage.dealingCard)
    players.foreach(x => x._2.outRef ! readyEvent)

    players.foreach(_._2.prepare())

    var deals = poker.deal()
    players.foreach {
      case (_, obj) =>
        obj.remainCards = deals.head
        val event = RoomEvent(
          message = RoomMessage.dealCard,
          cards = obj.remainCards
        )

        obj.outRef ! event

        deals = deals.tail
    }

    // 重新安排顺序
    playingSort = poker.playingSort(players)
    againstCards = List()
    playingIndex = 0

    // compete 叫牌
    val competeEvent = RoomEvent(
      message = RoomMessage.compete,
      playerId = playingSort(playingIndex),
      competeBoost = competeBoost
    )
    players.foreach(x => x._2.outRef ! competeEvent)
  }

  def check(playerId: Long, cards: List[Card]): Unit = {
    val player = players(playerId)

    if (poker.play(againstCards, cards)) {
      player.outRef ! RoomEvent(message = RoomMessage.checkSuccess)
    }
    else {
      player.outRef ! RoomEvent(message = RoomMessage.checkFail)
    }
  }

  def play(playerId: Long, cards: List[Card]): Unit = {
    //todo: 解除超时任务

    val event = RoomEvent(
      message = RoomMessage.played,
      playerId = playerId,
      cards = cards
    )
    (players - playerId).foreach(x => x._2.outRef ! event)

    //todo: 特效：炸弹

    // 通知下一家出牌
    againstCards = cards
    playingIndex = poker.nextPlayingIndex(playingIndex)
    val playingPlayerId = playingSort(playingIndex)
    val next = RoomEvent(
      message = RoomMessage.playing,
      playerId = playingPlayerId
    )
    players(playingPlayerId).outRef ! next

    //todo: 超时继续通知下一家
  }

  def hintPlay(playerId: Long): Unit = {
    val player = players(playerId)

    val hints = poker.hint(againstCards, player.remainCards)

    val event = RoomEvent(
      message = RoomMessage.hint,
      playerId = playerId,
      hints = hints
    )

    player.outRef ! event
  }

}

