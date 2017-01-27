package com.bizagi.gatling.executor

import com.bizagi.gatling.gradle.{Gradle, GradleProject, JvmArgs, Task}
import rx.lang.scala.Observable

/**
  * Created by dev-williame on 1/3/17.
  */
object Gatling {

  def execute(project: Project, script: Script, simulation: Simulation): Observable[Logs] =
    Gradle.execute(
      GradleProject(project.sources),
      Task(s"gatling-${script.script}"),
      JvmArgs(Map("config" -> createGatlingConfig(simulation.setup, simulation.hosts.hosts)))
    ).filterNot(_.equals("\n")).map(s => Logs(Seq(s)))

  private def createGatlingConfig(setup: String, host: Seq[String]): String = {
    s"""
       |{
       | "urls": [${host.map(v => s"""" $v"""").mkString(",")} ]
       | "setup": [
       |    $setup
       | ]
       |}
        """.stripMargin
  }
}

case class Project(sources: String) extends AnyVal

case class Script(script: String) extends AnyVal

case class Logs(logs: Seq[String]) extends AnyVal

trait Log
case class UserLog(value: String) extends Log
case class RequestLog(value: String) extends Log

case class Hosts(hosts: String*) extends AnyVal

case class Simulation(hosts: Hosts, setup: String)