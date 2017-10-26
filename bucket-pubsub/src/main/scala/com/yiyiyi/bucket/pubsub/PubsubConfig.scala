package com.yiyiyi.bucket.pubsub

import java.util

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

  object message {
    lazy val fullRoom: String = configuration.getString("pubsub.message.fullRoom")
    lazy val unAvailableRoom: String = configuration.getString("pubsub.message.unAvailableRoom")
  }

  object akka {
    lazy val roles: util.List[String] = configuration.getStringList("akka.cluster.roles")
  }

  object actor {
    private lazy val actorConfiguration = configuration.getConfig("pubsub.actor")

    lazy val timeout: Int = actorConfiguration.getInt("timeout")

    lazy val dashboardLifeHours: Int = actorConfiguration.getInt("dashboardLifeHours")
    lazy val roomLifeHours: Int = actorConfiguration.getInt("roomLifeHours")
    lazy val userLifeHours: Int = actorConfiguration.getInt("userLifeHours")

    lazy val overflowRate: Double = actorConfiguration.getDouble("overflowRate")

    lazy val roomCount: Int = actorConfiguration.getInt("roomCount")
    lazy val userCount: Int = actorConfiguration.getInt("userCount")

    lazy val dashBoardTick: Int = actorConfiguration.getInt("dashBoardTick")
    lazy val roomTick: Int = actorConfiguration.getInt("roomTick")
    lazy val userTick: Int = actorConfiguration.getInt("userTick")
  }

}
