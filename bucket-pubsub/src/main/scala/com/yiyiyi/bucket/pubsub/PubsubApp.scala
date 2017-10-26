package com.yiyiyi.bucket.pubsub

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.server.{ Directive0, Directives, ExceptionHandler, Route }
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.yiyiyi.bucket.pubsub.entity.{ DashBoardManager, RoomEntity }
import com.yiyiyi.bucket.pubsub.rest.{ ActorRest, WebSocketRest }
import org.slf4j.LoggerFactory

/**
 * @author xuejiao
 */
object PubsubApp extends App {
  implicit val system = ActorSystem("bucket-pubsub")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val log = LoggerFactory.getLogger(getClass)

  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case e: Exception =>
        log.error(s"Exception", e)
        Directives.complete(HttpResponse(InternalServerError, entity = "Internal Server Error"))
    }

  init()

  val route = Directives.respondWithHeaders(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Methods", "*"),
    RawHeader("Access-Control-Allow-Headerses", "*")
  ) {
      new RipakNgRoute(system, materializer).routeLogged
    }

  val host = PubsubConfig.app.host
  val port = PubsubConfig.app.port
  val source = Http().bind(host, port)
  log.info(s"$getClass started at $host:$port")

  source.runForeach { conn =>
    conn.handleWith(route)
  }

  def init(): Unit = {
    startPubSubCluster()
  }

  def startPubSubCluster()(implicit system: ActorSystem) {
    val role = Some(PubsubConfig.akka.roles.get(0))

    DashBoardManager.startManager(system, role)

    RoomEntity.startSharding
  }

}

final class RipakNgRoute(val system: ActorSystem, val materializer: ActorMaterializer)
    extends ActorRest with WebSocketRest {
  val timeout: Timeout = new Timeout(PubsubConfig.app.timeout, TimeUnit.SECONDS)

  def route: Route = actorApi ~ wsApi
  def routeLogged: Route = logAccess(route)
  private def logAccess: Directive0 = logRequestResult(accessLogFunc)

  private def accessLogFunc: (HttpRequest => (Any => Option[LogEntry])) = { req =>
    // we should remember request first, at here
    val requestTime = System.currentTimeMillis
    val func: Any => Option[LogEntry] = {
      case Complete(resp) =>
        val costTime = System.currentTimeMillis() - requestTime
        val contentLen = resp.entity.contentLengthOption.getOrElse(-1)
        val accessLog = s"AccessLog: ${req.method} ${req.uri} ${resp.status} $costTime $contentLen"
        log.info(accessLog)
        None
      case _ => None
    }
    func
  }
}
