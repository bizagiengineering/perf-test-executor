package com.bizagi.perftest

import java.io.File

import com.bizagi.gatling.gatling.Gatling
import com.bizagi.gatling.gatling.Gatling._
import com.bizagi.gatling.gatling.log.Log.PartialLog
import com.bizagi.gatling.gradle.Gradle
import com.bizagi.perftest.output.ConsoleOutput
import com.bizagi.perftest.serializar.JsonSerializer
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
      case "warmup" =>
        warmup(opts)
      case "load" =>
        load(opts)
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

  def getHost(host: String, config: Config): ValidationNel[ConfigError, HostConfig] =
    PerfTestConfigs.host(config, host)

  def getWarmupSimulation(scenario: String, config: Config): ValidationNel[ConfigError, ScenarioConfig] =
    PerfTestConfigs.warmupScenario(config, scenario)

  def getSimulation(scenario: String, setup: String, config: Config): ValidationNel[ConfigError, ScenarioConfig] =
    PerfTestConfigs.scenario(config, scenario, setup)

  def warmup(opts: OptsWrapper): Unit = {
    val value = (getConfig(opts) |@| getHost(opts) |@| getScenario(opts)) { (config, host, scenario) =>
      (getHost(host, config) |@| getWarmupSimulation(scenario, config)) ((_, _))
    }

    value.fold(println, m => m.fold(println, { params =>
      val (host, scenario) = params
      Gatling(
        project = Project(scenario.project),
        Script(scenario.scenario),
        Simulation(Hosts(host.hosts: _*), Setup(scenario.setup))
      ).run(Gradle).observable.foreach(l => println(JsonSerializer.toJson(l)), e => e.printStackTrace())
    }))
  }

  def load(opts: OptsWrapper): Unit = {
    val value = (getConfig(opts) |@| getHost(opts) |@| getScenario(opts) |@| getSetup(opts)) { (config, host, scenario, setup) =>
      (getHost(host, config) |@| getSimulation(scenario, setup, config)) ((_, _))
    }

    val producer = KafkaConnector.createProducer

    value.fold(println, m => m.fold(println, { params =>
      val (host, scenario) = params
      val observable = Gatling(
        project = Project(scenario.project),
        Script(scenario.scenario),
        Simulation(Hosts(host.hosts: _*), Setup(scenario.setup))
      ).run(Gradle).observable
      observable.observeOn(IOScheduler()).foreach(l => {
        l match {
          case p: PartialLog => println(ConsoleOutput.toOutput(p))
          case _ => println(l)
        }
      }, e => e.printStackTrace(), () => producer.close())
    }))
  }
}
