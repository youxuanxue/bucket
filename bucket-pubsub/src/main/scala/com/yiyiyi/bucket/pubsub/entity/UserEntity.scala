package com.yiyiyi.bucket.pubsub.entity

import akka.actor.Props

/**
 * @author xuejiao
 */
object UserEntity {
  def props() = Props(classOf[UserEntity])

  val typeName: String = getClass.getSimpleName
}

final class UserEntity {

}