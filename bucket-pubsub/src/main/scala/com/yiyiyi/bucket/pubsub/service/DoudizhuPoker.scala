package com.yiyiyi.bucket.pubsub.service
import com.yiyiyi.bucket.base.model.Card

/**
 * @author xuejiao
 */
class DoudizhuPoker extends Poker {
  override val playerNum: Int = 3
  override val hiddenNum: Int = 3
  override val pokerNum: Int = 1
  override val eachCardNum: Int = 17
  override val excludeCards: Set[Card] = Set()

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

