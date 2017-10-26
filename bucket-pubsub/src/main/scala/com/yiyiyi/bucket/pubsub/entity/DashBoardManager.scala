package com.yiyiyi.bucket.pubsub.entity

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Cancellable, PoisonPill, Props }
import akka.cluster.sharding.ClusterSharding
import akka.cluster.singleton.{ ClusterSingletonManager, ClusterSingletonManagerSettings }
import akka.util.Timeout
import com.yiyiyi.bucket.base
import com.yiyiyi.bucket.pubsub.{ ActiveCheckTick, NextTickExit, PubsubConfig }

import scala.concurrent.duration._
/**
 * @author xuejiao
 */
object DashBoardManager {
  def props() = Props(classOf[DashBoardManager])

  val managerName: String = "dashboard-manager"
  val managerPath: String = "/user/" + managerName
  val singletonName: String = "dashboard"
  val singletonPath: String = managerPath + "/" + singletonName

  def startManager(system: ActorSystem, role: Option[String]): ActorRef = {
    val settings = ClusterSingletonManagerSettings(system).withRole(role).withSingletonName(singletonName)
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = props(),
        terminationMessage = PoisonPill,
        settings = settings
      ), name = managerName
    )
  }

  def manager(implicit system: ActorSystem): ActorSelection = system.actorSelection(singletonPath)

  final case class ActorJoin(typeName: String, id: String)

  final case class ActorTick(typeName: String, id: String)

  final case class ActorExit(typeName: String, id: String)

  final case class ActorFail(typeName: String, id: String)

  final case class RequestJoin(requestInfo: RequestInfo)

  case object GetStatistic

}

final case class DashBoard(
    startTime: Long = System.currentTimeMillis(),
    startTimeDebug: String = base.dateFormat.format(new Date(System.currentTimeMillis())),
    lifeHours: Map[String, Int],
    var activeActors: Map[String, Map[String, Long]] = Map(),
    var failedActors: Map[String, Map[String, Long]] = Map(),
    var requests: Map[String, Vector[RequestInfo]] = Map(),
    var actorStatistics: Map[String, ActorStatistic] = Map(),
    var requestStatistics: Map[String, RequestStatistic] = Map()
) {
}

final case class ActorStatistic(typeName: String, activeCount: Long, failCount: Long)

final case class RequestInfo(
  name: String,
  timestamp: Long,
  costTime: Long,
  success: Boolean,
  contentLen: Long
)

final case class RequestStatistic(
  name: String,
  var latestRequestTime: Long = Long.MinValue,
  var averageCostTime: Double = 0.0,
  var latestCostTime: Long = 0L,
  var averageContentLen: Double = 0.0,
  var latestContentLen: Long = 0L,
  var totalCount: Long = 0,
  var successCount: Long = 0,
  var availability: Double = 0.0,
  var qps: Double = 0.0
)

class DashBoardManager extends Actor with ActorLogging {

  import DashBoardManager._

  private implicit val system = context.system
  private implicit val ec = system.dispatcher
  private implicit val timeout = new Timeout(PubsubConfig.actor.timeout.seconds)

  private val actorConf = PubsubConfig.actor

  private lazy val passionateTime = TimeUnit.HOURS.toMillis(PubsubConfig.actor.dashboardLifeHours)

  private val lifeHours = Map(
    RoomEntity.typeName -> actorConf.roomLifeHours,
    UserEntity.typeName -> actorConf.userLifeHours
  )

  private val maxCountMap = Map(
    RoomEntity.typeName -> actorConf.roomCount,
    UserEntity.typeName -> actorConf.userCount
  )

  private val dashBoard = DashBoard(lifeHours = lifeHours)

  private val scheduler = context.system.scheduler
  private val tickMinute = PubsubConfig.actor.dashBoardTick.minute
  private var activeCheckTask: Option[Cancellable] = Some(
    scheduler.schedule(tickMinute, tickMinute, self, ActiveCheckTick)
  )

  override def postStop() {
    activeCheckTask.foreach(_.cancel)
    activeCheckTask = None
    super.postStop()
  }

  override def receive: Receive = {
    case ActorJoin(typeName, id) =>
      val timestamp = System.currentTimeMillis()
      val actors = dashBoard.activeActors.getOrElse(typeName, Map()) ++ Map(id -> timestamp)
      dashBoard.activeActors ++= Map(typeName -> actors)

    case ActorTick(typeName, id) =>
      val timestamp = System.currentTimeMillis()
      val actors = dashBoard.activeActors.getOrElse(typeName, Map()) ++ Map(id -> timestamp)
      dashBoard.activeActors ++= Map(typeName -> actors)

    case ActorExit(typeName, id) =>
      val actors = dashBoard.activeActors.getOrElse(typeName, Map()) - id
      dashBoard.activeActors ++= Map(typeName -> actors)

    case ActorFail(typeName, id) =>
      val timestamp = System.currentTimeMillis()
      val actors = dashBoard.failedActors.getOrElse(typeName, Map()) ++ Map(id -> timestamp)
      dashBoard.failedActors ++= Map(typeName -> actors)

    case RequestJoin(requestInfo) =>
      val name = requestInfo.name
      val requests = dashBoard.requests.getOrElse(name, Vector[RequestInfo]()) :+ requestInfo
      dashBoard.requests ++= Map(name -> requests)

    case GetStatistic =>
      val commander = sender()
      computeStatistic(commander)

    case ActiveCheckTick =>
      checkTick()
  }

  private def computeStatistic(commander: ActorRef): Unit = {
    val activeMap = dashBoard.activeActors.map(x => (x._1, x._2.size.toLong))
    val failMap = dashBoard.failedActors.map(x => (x._1, x._2.size.toLong))
    dashBoard.actorStatistics = (activeMap.keySet ++ failMap.keySet).map { typeName =>
      val activeCount = activeMap.getOrElse(typeName, 0L)
      val failCount = failMap.getOrElse(typeName, 0L)
      (typeName, ActorStatistic(typeName, activeCount, failCount))
    }.toMap

    dashBoard.requestStatistics = dashBoard.requests.map {
      case (name, curRequests) =>
        val target = RequestStatistic(name = name)
        var sumCostTime: Long = 0
        var sumContentLen: Long = 0
        var oldestRequestTime: Long = Long.MaxValue

        curRequests.foreach { info =>
          if (oldestRequestTime > info.timestamp) {
            oldestRequestTime = info.timestamp
          }
          if (target.latestRequestTime < info.timestamp) {
            target.latestRequestTime = info.timestamp
            target.latestCostTime = info.costTime
            target.latestContentLen = info.contentLen
          }

          sumCostTime += info.costTime
          sumContentLen += info.contentLen

          target.totalCount += 1
          if (info.success) {
            target.successCount += 1
          }
        }

        if (target.totalCount > 0) {
          target.averageCostTime = sumCostTime / target.totalCount.toDouble
          target.averageContentLen = sumContentLen / target.totalCount.toDouble
          target.availability = target.successCount / target.totalCount.toDouble
        }

        val rangeSeconds = (target.latestRequestTime - oldestRequestTime) / 1000.0 + 1
        target.qps = target.totalCount / rangeSeconds

        (name, target)
    }

    commander ! dashBoard
  }

  private def checkTick(): Unit = {
    val minTimestamp = System.currentTimeMillis() - passionateTime
    dashBoard.activeActors.foreach {
      case (typeName, _) =>
        cleanActors(typeName)
    }

    dashBoard.failedActors = dashBoard.failedActors.map {
      case (typeName, actors) =>
        (typeName, actors.filter(x => x._2 > minTimestamp))
    }

    dashBoard.requests = dashBoard.requests.map {
      case (name, requests) =>
        (name, requests.filter(x => x.timestamp > minTimestamp))
    }
  }

  private def cleanActors(typeName: String): Unit = {
    var entities = dashBoard.activeActors.getOrElse(typeName, Map())

    val count = entities.size

    val maxCount: Int = maxCountMap.getOrElse(typeName, 0)

    var cleaning: Map[String, Long] = Map()

    if (maxCount > 0 && count > maxCount) {
      val overflowCount = count - (maxCount * actorConf.overflowRate).toInt

      cleaning = entities.toList.sortBy(_._2).take(overflowCount).toMap
      entities --= cleaning.keys
      dashBoard.activeActors ++= Map(typeName -> entities)

      log.info(s"$managerName will clean [${cleaning.size}] $typeName")
    }

    typeName match {
      case RoomEntity.typeName |
        UserEntity.typeName =>
        val sharding = ClusterSharding(system).shardRegion(typeName)
        cleaning.foreach {
          case (id, lastUpdateTime) =>
            sharding ! NextTickExit(id.toLong, lastUpdateTime)
        }
    }

  }

}
