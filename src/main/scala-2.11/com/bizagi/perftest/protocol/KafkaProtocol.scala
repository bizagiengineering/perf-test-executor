package com.bizagi.perftest.protocol

import java.util.Properties

import com.bizagi.gatling.gatling.Gatling
import com.bizagi.gatling.gatling.Gatling._
import com.bizagi.gatling.gatling.log.Log.Log
import com.bizagi.gatling.gradle.Gradle
import com.bizagi.perftest.{KafkaConfig, PerfTestConfigs}
import com.typesafe.config.Config
import configs.ConfigError
import net.liftweb.json.JsonAST.JField
import net.liftweb.json.{DefaultFormats, _}
import org.apache.kafka.clients.consumer.KafkaConsumer
import rx.lang.scala.Observable

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scalaz.{NonEmptyList, Validation}

/**
  * Created by dev-williame on 2/14/17.
  */
object KafkaProtocol {

  implicit val formats = DefaultFormats

  trait Protocol {
    val name: String
  }

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
    val consumer: KafkaConsumer[String, String] = createConsumer(kafka)

    Future {
      while (true) {
        val consumerRecord = consumer.poll(100).asScala.toList
        consumerRecord.foreach { r =>
          val json = parse(r.value())
          val name = json.find {
            case JField("name", _) => true
          }
          name.getOrElse("") match {
            case "StartTest" =>
              val startTest = json.extract[StartTest]
              val o = load(config, startTest)
              o.getOrElse(Observable.empty).foreach(println)
          }
        }
      }
    }
  }

  private def createConsumer(kafka: KafkaConfig) = {
    val props = new Properties()
    props.put("bootstrap.servers", s"${kafka.kafka.host}:${kafka.kafka.port}")
    props.put("group.id", "test")
    props.put("enable.auto.commit", "true")
    props.put("auto.commit.interval.ms", "1000")
    props.put("session.timeout.ms", "30000")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    val consumer = new KafkaConsumer[String, String](props)
    val topics = List(kafka.protocolTopic)
    consumer.subscribe(topics)
    consumer
  }

  case class StartTest(timestamp: Long, topic: String, host: String, scenario: String, setup: String) extends Protocol {
    val name = "StartTest"
  }
}
