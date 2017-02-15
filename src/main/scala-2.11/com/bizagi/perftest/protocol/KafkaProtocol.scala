package com.bizagi.perftest.protocol

import java.util.Properties

import com.bizagi.gatling.gatling.Gatling
import com.bizagi.gatling.gatling.Gatling._
import com.bizagi.gatling.gatling.log.Log.Log
import com.bizagi.gatling.gradle.Gradle
import com.bizagi.perftest.serializar.JsonSerializer
import com.bizagi.perftest.{KafkaConfig, PerfTestConfigs}
import com.typesafe.config.Config
import configs.ConfigError
import net.liftweb.json.{DefaultFormats, _}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import rx.lang.scala.Observable

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scalaz.{NonEmptyList, Validation}
import scala.concurrent.ExecutionContext.Implicits.global

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
    val consumer: KafkaConsumer[String, String] = createConsumer(kafka)

    while (true) {
      val consumerRecord = consumer.poll(100).asScala.toList
      consumerRecord.foreach { r =>
        val json = parse(r.value())
        val value = json.extractOpt[StartTest]

        val producer = createProducer(kafka)

        value match {
          case Some(startTest) =>
            load(config, startTest).getOrElse(Observable.empty).foreach(l => {
              producer.send(new ProducerRecord(startTest.topic, "key", JsonSerializer.toJson(l)))
            })
          case None => println("none")
        }
      }
    }
  }

  private def createConsumer(kafka: KafkaConfig) = {
    val props = new Properties()
    props.put("bootstrap.servers", s"${kafka.kafka.host}:${kafka.kafka.port}")
    props.put("group.id", s"test${System.currentTimeMillis()}")
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

  private def createProducer(kafka: KafkaConfig): KafkaProducer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", s"${kafka.kafka.host}:${kafka.kafka.port}")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    new KafkaProducer[String, String](props)
  }

  trait Protocol
  case class StartTest(timestamp: Long, topic: String, host: String, scenario: String, setup: String) extends Protocol
  object EmptyProtocol extends Protocol
}
