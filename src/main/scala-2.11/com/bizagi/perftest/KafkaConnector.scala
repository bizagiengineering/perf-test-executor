package com.bizagi.perftest

import java.util.Properties

import com.bizagi.gatling.gatling.log.Log.Log
import com.bizagi.perftest.serializar.JsonSerializer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

/**
  * Created by dev-williame on 2/13/17.
  */
object KafkaConnector {
  def createProducer: KafkaProducer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    new KafkaProducer[String, String](props)
  }

  def sendMessage(log: Log, topic: String, producer: KafkaProducer[String, String]): Unit = {
    producer.send(new ProducerRecord(topic, "key", JsonSerializer.toJson(log)))
  }
}
