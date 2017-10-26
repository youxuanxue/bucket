package com.yiyiyi.bucket.pubsub.entity

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, Stash }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import com.yiyiyi.bucket.pubsub.entity.model.RoomObj
import com.yiyiyi.bucket.pubsub.{ Command, KillActor, PubsubConfig }

/**
 * @author xuejiao
 */
object RoomEntity {
  def props() = Props(classOf[RoomEntity])

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

  case object OffRoom
  case object FullRoom
  case object WaitingRoom

  final case class PlayerIn(id: Long, playerId: Long) extends Command
  final case class PlayerOut(id: Long, playerId: Long) extends Command

  final case class DealCard(id: Long) extends Command
  final case class Play(id: Long, playerId: Long) extends Command
}

final class RoomEntity extends Actor with Stash with ActorLogging {
  import RoomEntity._

  private implicit val system = context.system
  private implicit val ec = system.dispatcher
  private val id = self.path.name.toLong

  private var obj = RoomObj(id)

  override def preStart(): Unit = {
    load()
  }

  override def postStop(): Unit = {
    obj = null
    super.postStop()
  }

  override def receive: Receive = checking

  private def common: Receive = {
    case KillActor(_) =>
      self ! PoisonPill
  }

  private def checking: Receive = common orElse {
    case OffRoom =>
      context.become(off)
      unstashAll()

    case FullRoom =>
      context.become(full)
      unstashAll()

    case WaitingRoom =>
      context.become(waiting)
      unstashAll()

    case _ =>
      stash()
  }

  private def off: Receive = common orElse {
    case _ =>
      sender() ! PubsubConfig.message.unAvailableRoom
  }

  private def full: Receive = common orElse {
    case PlayerIn(_, _) =>
      sender() ! PubsubConfig.message.fullRoom

    case PlayerOut(_, playerId) =>
      val commander = sender()
      // todo: 区分房主退出和普通人退出
      obj.players = obj.players.filter(_ != playerId)
      context.become(waiting)
      commander ! true

    case DealCard(_) =>

    case Play(_, playerId) =>

    case _ =>
      sender() ! false
  }

  private def waiting: Receive = common orElse {
    case PlayerIn(_, playerId) =>
      val commander = sender()
      obj.players += (playerId -> System.currentTimeMillis())
      if (obj.isFull) {
        context.become(full)
      }
      commander ! true

    case PlayerOut(_, playerId) =>
      val commander = sender()
      // todo: 区分房主退出和普通人退出
      obj.players = obj.players.filter(_ != playerId)
      if (obj.players.isEmpty) {
        context.become(off)
      }
      commander ! true

    case _ =>
      sender() ! false
  }

  private def load(): Unit = {
    // todo: loading

    if (obj.isAvailable) {
      if (obj.isFull) self ! FullRoom
      else self ! WaitingRoom
    }
    else self ! OffRoom
  }

}
