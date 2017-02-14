package com.bizagi.perftest.protocol

/**
  * Created by dev-williame on 2/14/17.
  */
object KafkaProtocol {

  trait Protocol {
    val name: String
  }
  case class StartTest(timestamp: Long, topic: String, host: String, scenario: String, setup: String) extends Protocol {
    val name = "StartTest"
  }

}
