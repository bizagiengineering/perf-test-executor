package com.bizagi.perftest

import java.io.File

import com.bizagi.gatling.gatling.Gatling
import com.bizagi.gatling.gatling.Gatling._
import com.bizagi.gatling.gradle.Gradle
import com.bizagi.perftest.protocol.KafkaProtocol
import com.typesafe.config.{Config, ConfigFactory}
import configs.ConfigError
import rx.lang.scala.schedulers.IOScheduler

import scala.util.Try
import scalaz.Scalaz._
import scalaz._

/**
  * Created by dev-williame on 2/11/17.
  */
object PerfTestApp extends App with DocoptApp {

  val doc =
    """
      |Usage:
      | perftest load --config CONFIG --host HOST --scenario SCENARIO --setup SETUP
      | perftest warmup --config CONFIG --host HOST --scenario SCENARIO
      | perftest distmode --config CONFIG
      |
      |Options:
      | --host HOST, -h HOST                      HostDocoptApp.scalaDocoptApp.scala
      | --scenario SCENARIO, -s SCENARIO          Gatling scenario
      | --config CONFIG, -c CONFIG                Configuration file
      | --setup SETUP, -e SETUP                   Simulation setup
      | --log LOG, -l LOG                         Log to be send to logstash
      | --port PORT, -p PORT                      Rest service port
      | --type TYPE, -t TYPE                      type load | warmup
      | --urls URLS, -u URLS                      Urls for the test
    """.stripMargin

  executeWithAppArgs(args, doc) { opts =>
    opts.option {
      case "load" =>
        load(opts)
      case "distmode" =>
        startDistMode(opts)
    }
  }

  def getConfig(opts: OptsWrapper): ValidationNel[String, Config] =
    opts.getAs[String]("--config").map { config =>
      Try {
        ConfigFactory.parseFile(new File(config))
      }.map(c => Success(c))
        .recover {
          case e: Exception => Failure(NonEmptyList(e.toString))
        }.get
    }.getOrElse(Failure(NonEmptyList("No config parameter provided")))

  def getHost(opts: OptsWrapper): ValidationNel[String, String] =
    opts.getAs[String]("--host").toSuccessNel("No host parameter provided")

  def getScenario(opts: OptsWrapper): ValidationNel[String, String] =
    opts.getAs[String]("--scenario").toSuccessNel("No scenario parameter provided")

  def getSetup(opts: OptsWrapper): ValidationNel[String, String] =
    opts.getAs[String]("--setup").toSuccessNel("No setup parameter provided")

  def getKafkaConfig(config: Config): ValidationNel[ConfigError, KafkaConfig] =
    PerfTestConfigs.kafka(config)

  def getProject(config: Config): ValidationNel[ConfigError, String] =
    PerfTestConfigs.project(config)

  def startDistMode(opts: OptsWrapper): Unit = {
    getConfig(opts)
      .map { config =>
        (config, getKafkaConfig(config))
      }
      .fold(error, m => {
        val (config, kafka) = m
        kafka.fold(error, k => {
          KafkaProtocol.startSlaveProtocol(config, k)
        })
      })
  }

  def load(opts: OptsWrapper): Unit = {
    val value = (getConfig(opts) |@| getHost(opts) |@| getScenario(opts) |@| getSetup(opts)) { (config, host, scenario, setup) =>
      getProject(config).map { project =>
        Gatling(project = Project(project), Script(scenario), Simulation(Hosts(host), Setup(setup)))
      }
    }
    value.fold(error, m => m.fold(error, { exec =>
      val observable = exec.run(Gradle).observable
      observable.observeOn(IOScheduler()).foreach(println)
    }))
  }

  def error[A] = (a: A) => println(a)
}
