package com.yiyiyi.bucket.pubsub.rest

import akka.http.scaladsl.server.Route

/**
 * @author xuejiao
 */
trait ActorRest extends Rest {

  def actorApi: Route = pathPrefix("v1") {
    path("ping") {
      get {
        complete("pong")
      }
    }
  }

}
