package com.yiyiyi.bucket.base.model

import com.yiyiyi.bucket.base.model.CardType.CardType

/**
 * @author xuejiao
 */
final case class Card(`type`: CardType, value: Int)

object Card {

  def compare(left: Card, right: Card): Int = {
    left.`type` match {
      case CardType.joker =>
        if (right.`type` == CardType.joker) {
          left.value.compareTo(right.value)
        }
        else 1

      case _ =>
        if (right.`type` == CardType.joker) -1
        else left.value.compareTo(right.value)
    }
  }

  def compare(left: Int, right: Int): Int = {
    left.compareTo(right)
  }

  def triableOne(cards: List[Card]): Int = {
    if (cards.size == 4) {
      val triableCard = cards.groupBy(_.value).filter {
        case (_, xs) =>
          xs.size == 3
      }
      triableCard.head._1
    }
    else -1
  }

  def triableTwo(cards: List[Card]): Int = {
    if (cards.size == 5) {
      val triableCard = cards.groupBy(_.value).filter {
        case (_, xs) =>
          xs.size == 3
      }

      triableCard.head._1
    }
    else -1
  }

  def fourthTwo(cards: List[Card]): Int = {
    if (cards.size == 6) {
      val fourthCard = cards.groupBy(_.value).filter {
        case (_, xs) =>
          xs.size == 4
      }
      fourthCard.head._1
    }
    else -1
  }

  def isTriable(cards: List[Card]): Boolean = {
    val values = cards.map(_.value).toSet
    cards.size == 3 && values.size == 1
  }

  def isDouble(cards: List[Card]): Boolean = {
    val values = cards.map(_.value).toSet
    cards.size == 2 && values.size == 1
  }

  def isBothJoker(cards: List[Card]): Boolean = {
    val cardTypes = cards.map(_.`type`).toSet
    cards.size == 2 && cardTypes.size == 1 && cardTypes.head == CardType.joker
  }

  def isBomb(cards: List[Card]): Boolean = {
    val values = cards.map(_.value).toSet
    cards.size == 4 && values.size == 1
  }

  def isSingleShunzi(cards: List[Card]): Boolean = {
    if (cards.isEmpty) false
    else {
      var repeated = cards.head.value
      var minValue = cards.head.value
      var maxValue = cards.head.value
      var count = 1
      cards.tail.foreach { x =>
        count += 1
        repeated &= x.value
        if (x.value > maxValue) maxValue = x.value
        else if (x.value < minValue) minValue = x.value
      }

      repeated == 0 && maxValue - minValue + 1 == count
    }
  }

  def isDoubleShunzi(cards: List[Card]): Boolean = {
    if (cards.isEmpty) false
    else {
      val groups = cards.groupBy(_.value)
      groups.size >= 3 && groups.forall(x => x._2.size == 2)
    }
  }

  def isTriableShunzi(cards: List[Card]): Boolean = {
    if (cards.isEmpty) false
    else {
      val groups = cards.groupBy(_.value)
      groups.size >= 2 && groups.forall(x => x._2.size == 3)
    }
  }

  def isFourShunzi(cards: List[Card]): Boolean = {
    if (cards.isEmpty) false
    else {
      val groups = cards.groupBy(_.value)
      groups.size >= 2 && groups.forall(x => x._2.size == 4)
    }
  }

}

object CardType extends Enumeration {
  type CardType = Value
  val spade = Value // 黑桃♠
  val heart = Value // 红心♥
  val club = Value // 梅花♣
  val diamond = Value // 方块♦

  val joker = Value // 鬼牌

  val numMap = Map(
    spade -> 13,
    heart -> 13,
    club -> 13,
    diamond -> 13,
    joker -> 2
  )
}

object CardAction extends Enumeration {
  type CardAction = Value
  val shuffle = Value //洗牌
  val cut = Value // 切牌
  val deal = Value //发牌
  val sort = Value // 理牌
  val draw = Value // 摸牌
  val play = Value // 打出
  val discard = Value //弃牌
  var compete = Value //叫牌／抢牌
}

object RoomStatus extends Enumeration {
  type RoomStatus = Value
  val off = Value(0)
  val on = Value(1)
}