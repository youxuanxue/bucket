package com.yiyiyi.bucket.pubsub.service

import com.yiyiyi.bucket.base.model.Card

/**
 * @author xuejiao
 */
trait Poker {
  val playerNum: Int
  val hiddenNum: Int // 暗牌数量

  var hiddenCards: List[Card] = List[Card]()
  val pokerNum: Int = 1 // 扑克牌的数量

  def shuffle(): List[Card]

  def deal(): List[List[Card]]

  def hint(against: List[Card], cards: List[Card]): List[List[Card]]

  def play(against: List[Card], cards: List[Card]): Boolean

}

object PokerType extends Enumeration {
  type PokerType = Value
  val unknown, dizhu, jinghua, banzi = Value

  val playerNumLimit = Map(
    unknown -> 0,
    dizhu -> 4,
    jinghua -> -1, // -1 表示可以随意的人数
    banzi -> 4
  )
}
