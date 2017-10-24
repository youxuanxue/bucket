package com.yiyiyi.bucket.pubsub.rest

/**
 * @author xuejiao
 */
trait ActorRest extends Rest {

  def actorApi = pathPrefix("v1") {
    path("ping") {
      get {
        complete("pong")
      }
    }
  }

}
