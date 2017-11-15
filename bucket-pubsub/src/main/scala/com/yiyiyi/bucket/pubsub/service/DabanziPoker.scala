package com.yiyiyi.bucket.pubsub.service

import com.yiyiyi.bucket.base.model.{ Card, CardType }

/**
 * @author xuejiao
 */
class DabanziPoker extends Poker {
  override val playerNum: Int = 4
  override val hiddenNum: Int = 0
  override val pokerNum: Int = 1
  override val eachCardNum: Int = 18
  override val excludeCards: Set[Card] = {
    Range.inclusive(1, CardType.numMap(CardType.joker))
      .map(value => Card(CardType.joker, value))
      .toSet
  }

  override def hint(against: List[Card], cards: List[Card]): List[List[Card]] = ???

  override def play(against: List[Card], cards: List[Card]): Boolean = ???
}
