package com.bizagi.perftest

import com.typesafe.config.{Config, ConfigList, ConfigObject, ConfigRenderOptions}
import configs.ConfigError
import configs.syntax._

import scalaz.Scalaz._
import scalaz._

/**
  * Created by dev-williame on 2/11/17.
  */
object PerfTestConfigs {
  def host(config: Config, name: String): ValidationNel[ConfigError, HostConfig] = {
    config.get[List[String]](s"hosts.$name.urls")
      .fold(failure, b => success(HostConfig(name, b)))
  }

  def scenario(config: Config, scenario: String, setup: String): ValidationNel[ConfigError, ScenarioConfig] = {
    val project = config.get[String](s"scenarios.$scenario.project").fold(failure, success)
    val scen = config.get[String](s"scenarios.$scenario.scenario").fold(failure, success)
    val set = config.get[ConfigList](s"scenarios.$scenario.setups.$setup").fold(failure, r => success(toJsonConfig(r)))

    (project |@| scen |@| set) { (p, s, e) =>
      ScenarioConfig(p, scenario, s, setup, e)
    }
  }

  def warmupScenario(config: Config, scenario: String): ValidationNel[ConfigError, ScenarioConfig] = {
    val project: ValidationNel[ConfigError, String] = config.get[String](s"scenarios.$scenario.project").fold(failure, success)
    val scen: ValidationNel[ConfigError, String] = config.get[String](s"scenarios.$scenario.scenario").fold(failure, success)
    val setup: ValidationNel[ConfigError, String] = config.get[ConfigList]("warmup").fold(failure, r => success(toJsonConfig(r)))

    (project |@| scen |@| setup) { (p, s, e) =>
      ScenarioConfig(p, scenario, s, "warump", e)
    }
  }

  private def failure = (e: ConfigError) => Failure(NonEmptyList(e))
  private def success[A] = (a: A) => Success(a)
  private def toJsonConfig(c: ConfigList) = c.render(ConfigRenderOptions.concise())
}

case class HostConfig(name: String, hosts: List[String])
case class ScenarioConfig(project: String, scenarioName: String, scenario: String, setupName: String, setup: String)

