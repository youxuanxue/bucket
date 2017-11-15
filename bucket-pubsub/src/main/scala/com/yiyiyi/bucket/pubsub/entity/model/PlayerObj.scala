package com.yiyiyi.bucket.pubsub.entity.model

import akka.actor.ActorRef
import com.yiyiyi.bucket.base.model.Card

/**
 * @author xuejiao
 */
final case class PlayerObj(
    id: Long,
    var index: Long,
    var outRef: ActorRef,
    var ready: Boolean = false,
    var competed: Boolean = false,
    var playedCards: Vector[List[Card]] = Vector(),
    var remainCards: List[Card] = List()
) {
  def prepare(): Unit = {
    playedCards = Vector()
    remainCards = List()
  }
}
