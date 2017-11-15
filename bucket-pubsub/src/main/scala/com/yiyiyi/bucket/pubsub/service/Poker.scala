package com.yiyiyi.bucket.pubsub.service

import com.yiyiyi.bucket.base.model.{ Card, CardType }
import com.yiyiyi.bucket.pubsub.entity.model.PlayerObj

import scala.collection.mutable
import scala.util.Random

/**
 * @author xuejiao
 */
trait Poker {
  val playerNum: Int // 玩家数量
  val hiddenNum: Int // 暗牌数量
  val pokerNum: Int // 扑克牌的数量
  val eachCardNum: Int // 普通玩家的牌数
  val excludeCards: Set[Card] // 废弃牌数量

  var hiddenCards: List[Card] = List[Card]()

  def hint(against: List[Card], cards: List[Card]): List[List[Card]]
  def play(against: List[Card], cards: List[Card]): Boolean

  def playingSort(players: Map[Long, PlayerObj]): List[Long] = {
    Random.shuffle(players.keys.toList)
  }

  def nextPlayingIndex(curIndex: Int): Int = (curIndex + 1) % playerNum

  def shuffle(): List[Card] = {
    val cards = mutable.HashSet[Card]()

    CardType.numMap.foreach {
      case (cardType, num) =>
        Range.inclusive(1, num).foreach(value => cards.add(Card(cardType, value)))
    }

    excludeCards.foreach(x => cards.remove(x))

    // todo： check 每个人的牌的最大值，保证不要太幸运。。。
    Random.shuffle(cards.toList)
  }

  def deal(): List[List[Card]] = {
    var shuffles = shuffle()

    var deals = List[List[Card]]()
    var count = 0
    while (shuffles.nonEmpty && count < playerNum) {
      deals +:= shuffles.take(eachCardNum)
      shuffles = shuffles.drop(eachCardNum)
      count += 1
    }

    hiddenCards = shuffles

    deals
  }

}

object PokerType extends Enumeration {
  type PokerType = Value
  val unknown, dizhu, jinghua, banzi = Value
}
