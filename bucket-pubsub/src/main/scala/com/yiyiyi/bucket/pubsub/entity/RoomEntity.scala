package com.yiyiyi.bucket.pubsub.entity

import java.util.concurrent.TimeUnit

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, PoisonPill, Props, Stash }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import com.yiyiyi.bucket.base.model.{ Card, RoomStatus }
import com.yiyiyi.bucket.pubsub.entity.model.{ PlayerObj, RoomObj }
import com.yiyiyi.bucket.pubsub.model.{ RoomIncomingEvent, RoomIncomingMessage, RoomOutgoingEvent, RoomOutgoingMessage }
import com.yiyiyi.bucket.pubsub.service.DizhuPoker
import com.yiyiyi.bucket.pubsub._

import scala.concurrent.duration._

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
  case object Nothing

  final case class PlayerIn(playerId: Long, outRef: ActorRef)
  final case class PlayerOut(playerId: Long)
  final case class PlayerReady(playerId: Long)

  final case class Sink(id: Long, playerId: Long, event: RoomIncomingEvent) extends Command
  final case class Source(id: Long, playerId: Long, outRef: ActorRef) extends Command

  final case class DealCard(id: Long, playerId: Long) extends Command

  final case class PlayingIndex(id: Long) extends Command

  final case class HintPlay(id: Long) extends Command
  final case class CheckPlay(id: Long, cards: List[Card]) extends Command
  final case class Play(id: Long, cards: List[Card]) extends Command
}

final class RoomEntity extends Actor with Stash with ActorLogging {
  import RoomEntity._

  private implicit val system = context.system
  private implicit val ec = system.dispatcher
  private val id = self.path.name.toLong

  private var obj: RoomObj = null

  private var exitInNextTick: ExitInTickInfo = ExitInTickInfo()
  private var lastActiveTime = System.currentTimeMillis()
  private lazy val passionateTime = TimeUnit.HOURS.toMillis(PubsubConfig.actor.roomLifeHours)

  private val scheduler = context.system.scheduler
  private val tickMinute = PubsubConfig.actor.roomTick.minute
  private var activeCheckTask: Option[Cancellable] = Some(
    scheduler.schedule(tickMinute, tickMinute, self, ActiveCheckTick)
  )

  private lazy val dashBoardManager = DashBoardManager.manager

  override def postStop(): Unit = {
    dashBoardManager ! DashBoardManager.ActorExit(typeName, id.toString)
    obj = null
    super.postStop()
  }

  override def preStart(): Unit = {
    super.preStart()
    dashBoardManager ! DashBoardManager.ActorJoin(typeName, id.toString)
    getOrCreate()
  }

  def playerRef(playerId: Long): ActorRef = {
    val actorName = s"$playerId"
    val playerRefOpt = context.child(actorName)
    if (playerRefOpt.isDefined) {
      playerRefOpt.get
    }
    else {
      synchronized {
        val againPlayerRefOpt = context.child(actorName)
        if (againPlayerRefOpt.isDefined) againPlayerRefOpt.get
        else context.actorOf(PlayerEntity.props(context), actorName)
      }
    }
  }

  override def receive: Receive = checking

  private def common: Receive = {
    case KillActor(_) =>
      self ! PoisonPill

    case Sink(_, playerId, event) =>
      event.message match {
        case RoomIncomingMessage.ready =>
          self ! PlayerReady(playerId)

        case RoomIncomingMessage.leave =>
          self ! PlayerOut(playerId)

        case x =>
          log.warning(s"unsupported RoomIncomingMessage: ${x.toString}")
      }

    case Source(_, playerId, outRef) =>
      self ! PlayerIn(playerId, outRef)
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
    case PlayerIn(_, outRef) =>
      updateActiveEntity()
      if (outRef != null) {
        outRef ! RoomOutgoingEvent(message = RoomOutgoingMessage.fullRoom)
      }

    case PlayerOut(playerId) =>
      updateActiveEntity()
      playerLeft(playerId)

    case PlayerReady(playerId) =>
      updateActiveEntity()
      playerReady(playerId)

    case DealCard(_, playerId) =>
      val commander = sender()
      val cards = obj.players(playerId).remainCards
      commander ! cards

    case PlayingIndex(_) =>
      sender() ! obj.curPlayingIndex

    case HintPlay(_) =>
      val commander = sender()
      commander ! obj.hintPlay()

    case CheckPlay(_, cards) =>
      val commander = sender()
      val isValid = obj.check(cards)
      commander ! isValid

    case Play(_, cards) =>
      val commander = sender()
      obj.play(cards)
      commander ! true

    case _ =>
      sender() ! false
  }

  private def waiting: Receive = common orElse {
    case PlayerIn(playerId, outRef) =>
      updateActiveEntity()
      playerJoined(playerId, outRef)

    case PlayerOut(playerId) =>
      updateActiveEntity()
      playerLeft(playerId)

    case _ =>
      sender() ! false
  }

  private def updateActiveEntity(): Unit = {
    lastActiveTime = System.currentTimeMillis()
    dashBoardManager ! DashBoardManager.ActorTick(typeName, id.toString)
  }

  private def getOrCreate(): Unit = {
    // todo: loading
    obj = RoomObj(id, id, new DizhuPoker(), status = RoomStatus.on)

    if (obj.isAvailable) {
      if (obj.isFull) self ! FullRoom
      else self ! WaitingRoom
    }
    else self ! OffRoom
  }

  private def playerJoined(playerId: Long, outRef: ActorRef): Unit = {
    val player = PlayerObj(playerId, System.currentTimeMillis(), outRef)
    obj.players += (playerId -> player)

    val event = RoomOutgoingEvent(
      playerId = playerId,
      message = RoomOutgoingMessage.fullRoom
    )
    obj.players.foreach(x => x._2.outRef ! event)

    if (obj.isFull) context.become(full)
  }

  private def playerReady(playerId: Long): Unit = {
    obj.players(playerId).ready = true

    val allReady = obj.players.forall(x => x._2.ready)
    val event = if (allReady) {
      obj.deal()
      RoomOutgoingEvent(message = RoomOutgoingMessage.dealCard)
    }
    else {
      RoomOutgoingEvent(
        playerId = playerId,
        message = RoomOutgoingMessage.ready
      )
    }

    obj.players.foreach(x => x._2.outRef ! event)
  }

  private def playerLeft(playerId: Long): Unit = {
    // todo: 区分房主退出和普通人退出
    var playerObj = obj.players.get(playerId)
    obj.players -= playerId

    playerObj = None

    val event = RoomOutgoingEvent(
      playerId = playerId,
      message = RoomOutgoingMessage.leave
    )
    obj.players.foreach(x => x._2.outRef ! event)

    // todo： 复杂的状态
    if (obj.players.isEmpty) {
      context.become(off)
    }
    else {
      context.become(waiting)
    }

  }

}
