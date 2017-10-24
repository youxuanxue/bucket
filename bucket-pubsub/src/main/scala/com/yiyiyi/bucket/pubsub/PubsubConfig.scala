package com.yiyiyi.bucket.pubsub

import com.typesafe.config.{ Config, ConfigFactory }

/**
 * @author xuejiao
 */
object PubsubConfig {
  val env: String = System.getProperty("env", "debug")
  var configuration: Config = ConfigFactory.load()

  object app {
    lazy val host: String = configuration.getString("pubsub.app.host")
    lazy val port: Int = configuration.getInt(s"pubsub.app.port")
    lazy val timeout: Int = configuration.getInt("pubsub.app.timeout")
  }
}
