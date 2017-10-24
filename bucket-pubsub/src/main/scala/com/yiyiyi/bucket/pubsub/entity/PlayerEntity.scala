package com.yiyiyi.bucket.pubsub.entity

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import com.yiyiyi.bucket.pubsub.{ Command, KillActor }

/**
 * @author xuejiao
 */
object PlayerEntity {
  def props() = Props(classOf[PlayerEntity])

  val typeName: String = getClass.getSimpleName

  private val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command => (cmd.id.toString, cmd)
  }

  private val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command => (cmd.id.hashCode % 100).toString
  }

  def startSharding(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system)
      .start(typeName, props(), ClusterShardingSettings(system), extractEntityId, extractShardId)

  def startShardingProxy(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system).startProxy(typeName, None, extractEntityId, extractShardId)

}

final class PlayerEntity extends Actor with ActorLogging {
  private implicit val system = context.system
  private implicit val ec = system.dispatcher
  private val id = self.path.name.toLong

  override def receive: Receive = {
    case KillActor(_) =>
  }
}
