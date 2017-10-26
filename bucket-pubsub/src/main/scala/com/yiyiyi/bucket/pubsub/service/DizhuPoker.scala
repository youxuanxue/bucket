package com.yiyiyi.bucket.pubsub.service
import com.yiyiyi.bucket.base.model.{ Card, CardType }

import scala.util.Random

/**
 * @author xuejiao
 */
class DizhuPoker extends Poker {
  override val playerNum: Int = 3
  override val hiddenNum: Int = 3

  override def shuffle(): List[Card] = {
    var cards = List[Card]()

    CardType.commons.foreach { curType =>
      var value = 3 // 从 3 开始，A.value = 14 2.value = 15. 方便排面值的大小计算
      var count = 0
      while (count < CardType.numPerCard) {
        cards +:= Card(curType, value)
        value += 1
        count += 1
      }
    }

    cards ++= Range.inclusive(1, CardType.numPerJoker * pokerNum)
      .map(x => Card(CardType.joker, x))

    // todo： check 每个人的牌的最大值，保证不要太幸运。。。

    Random.shuffle(cards)
  }

  override def deal(): List[List[Card]] = {
    var shuffles = shuffle()

    val cardNumPerPlayer = (shuffles.size - hiddenNum) / playerNum

    hiddenCards = shuffles.take(hiddenNum)
    shuffles = shuffles.drop(hiddenNum)

    var deals = List[List[Card]]()
    while (shuffles.nonEmpty) {
      deals +:= shuffles.take(cardNumPerPlayer)
      shuffles = shuffles.drop(cardNumPerPlayer)
    }

    deals
  }

  override def hint(against: List[Card], cards: List[Card]): List[List[Card]] = {
    List()
  }

  override def play(against: List[Card], cards: List[Card]): Boolean = {
    val cardNum = against.size

    cardNum match {
      case 1 =>
        if (cards.size == cardNum) Card.compare(against.head, cards.head) < 0
        else Card.isBothJoker(cards) || Card.isBomb(cards)

      case 2 =>
        if (Card.isBothJoker(cards)) true
        else if (cards.size == cardNum) {
          Card.isDouble(cards) && Card.compare(against.head, cards.head) < 0
        }
        else Card.isBomb(cards)

      case 3 =>
        if (cards.size == cardNum) {
          Card.isTriable(cards) && Card.compare(against.head, cards.head) < 0
        }
        else {
          Card.isBothJoker(cards) || Card.isBomb(cards)
        }

      case 4 =>
        if (Card.isBothJoker(cards)) true
        else if (Card.isBomb(against)) {
          Card.isBomb(cards) && Card.compare(against.head, cards.head) < 0
        }
        else {
          Card.isBomb(cards) || Card.compare(Card.triableOne(against), Card.triableOne(cards)) < 0
        }

      case 5 =>
        if (cards.size == cardNum) {
          if (Card.isSingleShunzi(against) && Card.isSingleShunzi(cards)) {
            Card.compare(against.minBy(_.value), cards.minBy(_.value)) < 0
          }
          else {
            Card.compare(Card.triableTwo(against), Card.triableTwo(cards)) < 0
          }
        }
        else Card.isBomb(cards) || Card.isBothJoker(cards)

      case 6 =>
        if (cards.size == cardNum) {
          if (Card.isSingleShunzi(against) && Card.isSingleShunzi(cards) ||
            Card.isDoubleShunzi(against) && Card.isDoubleShunzi(cards) ||
            Card.isTriableShunzi(against) && Card.isTriableShunzi(cards)) {
            Card.compare(against.minBy(_.value), cards.minBy(_.value)) < 0
          }
          else {
            Card.compare(Card.fourthTwo(against), Card.fourthTwo(cards)) < 0
          }
        }
        else Card.isBothJoker(cards)

      case 7 =>
        if (cards.size == cardNum) {
          if (Card.isSingleShunzi(against) && Card.isSingleShunzi(cards)) {
            Card.compare(against.minBy(_.value), cards.minBy(_.value)) < 0
          }
          else false
        }
        else Card.isBomb(cards) || Card.isBothJoker(cards)

      case 8 =>
        if (cards.size == cardNum) {
          if (Card.isSingleShunzi(against) && Card.isSingleShunzi(cards) ||
            Card.isDoubleShunzi(against) && Card.isDoubleShunzi(cards) ||
            Card.isFourShunzi(against) && Card.isFourShunzi(cards)) {
            // todo: fourShunzi maybe bigger than bomb
            Card.compare(against.minBy(_.value), cards.minBy(_.value)) < 0
          }
          else {
            Card.compare(Card.triableTwo(against), Card.triableTwo(cards)) < 0
          }
        }
        else Card.isBomb(cards) || Card.isBothJoker(cards)

      case 9 =>
        if (cards.size == cardNum) {
          if (Card.isSingleShunzi(against) && Card.isSingleShunzi(cards) ||
            Card.isTriableShunzi(against) && Card.isTriableShunzi(cards)) {
            Card.compare(against.minBy(_.value), cards.minBy(_.value)) < 0
          }
          else false
        }
        else Card.isBomb(cards) || Card.isBothJoker(cards)

      case 10 =>
        if (cards.size == cardNum) {
          if (Card.isSingleShunzi(against) && Card.isSingleShunzi(cards) ||
            Card.isDoubleShunzi(against) && Card.isDoubleShunzi(cards)) {
            Card.compare(against.minBy(_.value), cards.minBy(_.value)) < 0
          }
          else false
        }
        else Card.isBomb(cards) || Card.isBothJoker(cards)

      case 11 =>
        if (cards.size == cardNum) {
          if (Card.isSingleShunzi(against) && Card.isSingleShunzi(cards)) {
            Card.compare(against.minBy(_.value), cards.minBy(_.value)) < 0
          }
          else false
        }
        else Card.isBomb(cards) || Card.isBothJoker(cards)

      case 12 =>
        if (cards.size == cardNum) {
          if (Card.isSingleShunzi(against) && Card.isSingleShunzi(cards) ||
            Card.isDoubleShunzi(against) && Card.isDoubleShunzi(cards) ||
            Card.isTriableShunzi(against) && Card.isTriableShunzi(cards)) {
            Card.compare(against.minBy(_.value), cards.minBy(_.value)) < 0
          }
          else false
        }
        else Card.isBomb(cards) || Card.isBothJoker(cards)

      case _ =>
        false
    }

  }

}

object DizhuPokerTest {
  def main(args: Array[String]): Unit = {
    val poker = new DizhuPoker()
    val cards = poker.deal()

    cards.foreach(x => println(x))
  }
}

