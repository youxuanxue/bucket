package com.yiyiyi.bucket.pubsub.entity.model

import com.yiyiyi.bucket.base.model.RoomStatus.RoomStatus
import com.yiyiyi.bucket.base.model.{ Card, RoomStatus }
import com.yiyiyi.bucket.pubsub.service.Poker

import scala.util.Random

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
    var curPlayingIndex: Int = -1,
    var againstCards: List[Card] = List()
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
    players.foreach(_._2.prepare())

    var deals = poker.deal()
    players.foreach {
      case (_, obj) =>
        obj.remainCards = deals.head
        deals = deals.tail
    }

    // todo: 重新安排顺序
    playingSort = Random.shuffle(players.keys.toList)
    curPlayingIndex = 0
    againstCards = List()
  }

  def check(cards: List[Card]): Boolean = {
    if (poker.play(againstCards, cards)) {
      true
    }
    else false
  }

  def play(cards: List[Card]): Unit = {
    againstCards = cards
    curPlayingIndex = (curPlayingIndex + 1) % players.size
  }

  def hintPlay(): List[List[Card]] = {
    val player = players(curPlayingIndex)

    val hints = poker.hint(againstCards, player.remainCards)

    hints
  }

}

