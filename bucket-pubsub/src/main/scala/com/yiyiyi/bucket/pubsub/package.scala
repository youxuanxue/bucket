package com.yiyiyi.bucket

/**
 * @author xuejiao
 */
package object pubsub {
  trait Command extends Serializable {
    def id: Long
  }

  trait StrCommand extends Serializable {
    def id: String
  }

  private[pubsub] final case class KillActor(id: Long) extends Command
  final case class NextTickExit(id: Long, lastUpdateTime: Long) extends Command

  final case class ExitInTickInfo(willExit: Boolean = false, timestamp: Long = 0)

  private[pubsub] case object Nothing
  private[pubsub] case object ActiveCheckTick
  private[pubsub] case object ReloadSucceed
  private[pubsub] case object ReloadFailed

}
