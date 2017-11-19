package com.yiyiyi.bucket.pubsub.entity

import java.util.concurrent.TimeUnit

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, PoisonPill, Props, Stash }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import com.yiyiyi.bucket.base.model.{ Card, RoomStatus }
import com.yiyiyi.bucket.pubsub._
import com.yiyiyi.bucket.pubsub.entity.model.{ PlayerObj, RoomObj }
import com.yiyiyi.bucket.pubsub.model.{ RoomEvent, RoomMessage }
import com.yiyiyi.bucket.pubsub.service.DoudizhuPoker

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

  final case class Sink(id: Long, playerId: Long, event: RoomEvent) extends Command
  final case class Source(id: Long, playerId: Long, outRef: ActorRef) extends Command

  final case class PlayerIn(playerId: Long, outRef: ActorRef)
  final case class PlayerOut(playerId: Long)
  final case class PlayerReady(playerId: Long)
  final case class Compete(id: Long, playerId: Long, competed: Boolean, boost: Int) extends Command

  final case class HintPlay(id: Long, playerId: Long) extends Command
  final case class CheckPlay(id: Long, playerId: Long, cards: List[Card]) extends Command
  final case class Play(id: Long, playerId: Long, cards: List[Card]) extends Command
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
        case RoomMessage.ready =>
          self ! PlayerReady(playerId)

        case RoomMessage.leave =>
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
      context.become(playing)
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

  private def playing: Receive = common orElse {
    case PlayerIn(_, outRef) =>
      updateActiveEntity()
      if (outRef != null) {
        outRef ! RoomEvent(message = RoomMessage.fullRoom)
      }

    case PlayerOut(playerId) =>
      updateActiveEntity()
      playerLeft(playerId)

    case PlayerReady(playerId) =>
      updateActiveEntity()
      playerReady(playerId)
      if (obj.players.forall(x => x._2.ready)) {
        obj.deal()
      }

    case Compete(_, playerId, competed, boost) =>
      updateActiveEntity()
      compete(playerId, competed, boost)

    case HintPlay(_, playerId) =>
      updateActiveEntity()
      obj.hintPlay(playerId)

    case CheckPlay(_, playerId, cards) =>
      updateActiveEntity()
      obj.check(playerId, cards)

    case Play(_, playerId, cards) =>
      updateActiveEntity()
      obj.play(playerId, cards)

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

    case PlayerReady(playerId) =>
      updateActiveEntity()
      playerReady(playerId)

    case _ =>
      sender() ! false
  }

  private def updateActiveEntity(): Unit = {
    lastActiveTime = System.currentTimeMillis()
    dashBoardManager ! DashBoardManager.ActorTick(typeName, id.toString)
  }

  private def getOrCreate(): Unit = {
    // todo: loading
    obj = RoomObj(id, id, new DoudizhuPoker(), status = RoomStatus.on)

    if (obj.isAvailable) {
      if (obj.isFull) self ! FullRoom
      else self ! WaitingRoom
    }
    else self ! OffRoom
  }

  private def playerJoined(playerId: Long, outRef: ActorRef): Unit = {
    val player = PlayerObj(playerId, System.currentTimeMillis(), outRef)
    obj.players += (playerId -> player)

    val event = RoomEvent(
      playerId = playerId,
      message = RoomMessage.join
    )
    obj.players.foreach(x => x._2.outRef ! event)

    if (obj.isFull) context.become(playing)
  }

  private def playerReady(playerId: Long): Unit = {
    obj.players(playerId).ready = true

    val readyEvent = RoomEvent(
      playerId = playerId,
      message = RoomMessage.ready
    )
    obj.players.foreach(x => x._2.outRef ! readyEvent)
  }

  private def playerLeft(playerId: Long): Unit = {
    // todo: 区分房主退出和普通人退出
    var playerObj = obj.players.get(playerId)
    obj.players -= playerId

    playerObj = None

    val event = RoomEvent(
      playerId = playerId,
      message = RoomMessage.leave
    )
    obj.players.foreach(x => x._2.outRef ! event)
  }

  private def compete(playerId: Long, competed: Boolean, boost: Int): Unit = {
    obj.players(playerId).competed = competed

    if (competed) {
      obj.competeBoost = boost
    }

    val allCompeted = obj.players.forall(x => x._2.competed)

    val event = if (allCompeted) {
      // 根据抢牌结果重新制定顺序
      obj.playingSort = obj.poker.playingSort(obj.players)
      obj.playingIndex = 0
      val playingPlayerId = obj.playingSort(obj.playingIndex)
      RoomEvent(
        message = RoomMessage.play,
        playerId = playingPlayerId
      )
    }
    else {
      obj.playingIndex = obj.poker.nextPlayingIndex(obj.playingIndex)
      val playingPlayerId = obj.playingSort(obj.playingIndex)
      RoomEvent(
        message = RoomMessage.compete,
        playerId = playingPlayerId,
        competeBoost = if (obj.competeBoost > 0) obj.competeBoost * 2 else 1 // 抢牌加倍
      )
    }

    obj.players.foreach(x => x._2.outRef ! event)
  }

}
