package com.bizagi.gatling.executor

import com.bizagi.gatling.gradle
import com.bizagi.gatling.gradle.Gradle

import scala.util.Try
import scalaz.effect.IO

/**
  * Created by dev-williame on 1/3/17.
  */
object Gatling {

  //  def executeAndGet(project: gradle.Project, script: Script, hosts: Hosts): IO[Logs] = ???
  //    for {
  //      output <- execute(project, script, hosts)
  //      logs <- SimulationLogs(output)
  //    } yield logs

  //  def execute(project: gradle.Project, script: Script, hosts: Hosts): IO[Try[SimulationOutput]] =
  //    Gradle.execute(
  //      Gradle(
  //        sources = project.sources,
  //        task = s"gatling-${script.script}",
  //        arguments = s"-Dconfig=${createGatlingConfig("", hosts.hosts)}")
  //    ).map { stream =>
  //      stream.map(_.toString().split("\n"))
  //        .map(_.find(_.contains("file:")))
  //        .map(_.replace("Please open the following file: ", ""))
  //        .map(_.replace("/index.html", "/simulation.log"))
  //        .map(SimulationOutput)
  //        .getOrElse(SimulationOutput(""))
  //    }

  def execute(project: Project, script: Script, simulation: Simulation): IO[Logs] = ???




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

case class Hosts(hosts: String*) extends AnyVal

case class Simulation(hosts: Hosts, setup: String)


