package com.yiyiyi.bucket.pubsub

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }

/**
 * @author xuejiao
 */
object PubsubBoard extends ExtensionId[PubsubBoardExtension] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): PubsubBoardExtension =
    new PubsubBoardExtension()(system)

  override def lookup(): ExtensionId[_ <: Extension] = PubsubBoard
}

class PubsubBoardExtension(implicit val system: ExtendedActorSystem) extends Extension {

}
