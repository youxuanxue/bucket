package com.yiyiyi.bucket.pubsub.entity

import akka.actor.{ Actor, ActorContext, ActorLogging, ActorRef, Props }
import com.yiyiyi.bucket.pubsub.KillActor
import com.yiyiyi.bucket.pubsub.entity.PlayerEntity.DealCard

/**
 * @author xuejiao
 */
object PlayerEntity {
  def props(roomCtx: ActorContext) = Props.create(classOf[PlayerEntity], roomCtx)

  val typeName: String = getClass.getSimpleName

  case object DealCard
  final case class Joined(ref: ActorRef)
  final case class StatusMessage(text: String)
}

final class PlayerEntity extends Actor with ActorLogging {
  private implicit val system = context.system
  private implicit val ec = system.dispatcher
  private val id = self.path.name.toLong

  override def receive: Receive = {
    case KillActor(_) =>

    case DealCard =>

  }
}
