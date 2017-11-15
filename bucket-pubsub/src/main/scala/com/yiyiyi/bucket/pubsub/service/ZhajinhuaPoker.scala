package com.yiyiyi.bucket.pubsub.service

import com.yiyiyi.bucket.base.model.{ Card, CardType }

/**
 * @author xuejiao
 */
class ZhajinhuaPoker extends Poker {
  override val playerNum: Int = -1
  override val hiddenNum: Int = -1
  override val pokerNum: Int = 1
  override val eachCardNum: Int = 3
  override val excludeCards: Set[Card] = {
    Range.inclusive(1, CardType.numMap(CardType.joker))
      .map(value => Card(CardType.joker, value))
      .toSet
  }

  override def hint(against: List[Card], cards: List[Card]): List[List[Card]] = ???

  override def play(against: List[Card], cards: List[Card]): Boolean = ???
}
