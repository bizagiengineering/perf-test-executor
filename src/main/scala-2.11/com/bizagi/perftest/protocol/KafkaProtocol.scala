package com.bizagi.perftest.protocol

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.{ActorMaterializer, ThrottleMode}
import com.bizagi.gatling.gatling.Gatling
import com.bizagi.gatling.gatling.Gatling._
import com.bizagi.gatling.gatling.log.Log.Log
import com.bizagi.gatling.gradle.Gradle
import com.bizagi.perftest.{KafkaConfig, PerfTestConfigs}
import com.typesafe.config.Config
import configs.ConfigError
import net.liftweb.json.{DefaultFormats, _}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import rx.lang.scala.Observable

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}
import scalaz.{NonEmptyList, Validation}

/**
  * Created by dev-williame on 2/14/17.
  */
object KafkaProtocol {

  implicit val formats = DefaultFormats

  def load(config: Config, startTest: StartTest): Validation[NonEmptyList[ConfigError], Observable[Log]] = {
    PerfTestConfigs.project(config).map { project =>
      Gatling(
        Project(project),
        script = Script(startTest.scenario),
        simulation = Simulation(Hosts(startTest.host), Setup(startTest.setup))
      ).run(Gradle).observable
    }
  }

  def startSlaveProtocol(config: Config, kafka: KafkaConfig) = {

    implicit val system = ActorSystem("HelloWorld")
    implicit val materializer = ActorMaterializer()

    val settings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(s"${kafka.kafka.host}:${kafka.kafka.port}")
      .withGroupId(s"group1${UUID.randomUUID()}")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

    val future = Consumer.plainSource(settings, Subscriptions.topics(kafka.protocolTopic))
      .throttle(1, 500 millis, 1, ThrottleMode.shaping)
      .map { r =>
        parse(r.value()).extractOpt[StartTest].getOrElse(EmptyProtocol)
      }
      .runForeach {
        case s: StartTest =>
          load(config, s)
      }

    Await.ready(future, Duration.Inf)
  }

  trait Protocol
  case class StartTest(timestamp: Long, topic: String, host: String, scenario: String, setup: String) extends Protocol
  object EmptyProtocol extends Protocol
}
