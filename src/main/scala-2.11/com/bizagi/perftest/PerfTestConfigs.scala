package com.bizagi.perftest

import com.typesafe.config.{Config, ConfigList, ConfigRenderOptions}
import configs.ConfigError
import configs.syntax._

import scalaz._

/**
  * Created by dev-williame on 2/11/17.
  */
object PerfTestConfigs {

  def kafka(config: Config): ValidationNel[ConfigError, KafkaConfig] = {
    config.get[KafkaConfig]("remote").fold(failure, success)
  }

  def project(config: Config): ValidationNel[ConfigError, String] = {
    config.get[String]("project").fold(failure, success)
  }

  private def failure = (e: ConfigError) => Failure(NonEmptyList(e))
  private def success[A] = (a: A) => Success(a)
}

case class KafkaConfig(kafka: Host, zookeper: Host, protocolTopic: String)
case class Host(host: String, port: Int)

