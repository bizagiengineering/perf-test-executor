package com.bizagi.gatling.gatling

import com.bizagi.gatling.executor.Exec
import com.bizagi.gatling.gatling.Gatling.{Project, Script, Simulation}
import com.bizagi.gatling.gatling.log.Log._
import com.bizagi.gatling.gatling.parser.LogParser
import com.bizagi.gatling.gradle._
import com.bizagi.gatling.grouper.ObservableGrouper._
import rx.lang.scala.Observable

import scala.concurrent.duration._
import scalaz.Reader

/**
  * Created by dev-williame on 1/3/17.
  */
trait Gatling {
  def apply(project: Project, script: Script, simulation: Simulation): Reader[Gradle, Observable[Log]]
  def cancel()
}

object Gatling {

  val DELIMITER: String = "=" * 80
  val SCRIPT_ERROR = "can not be found in the classpath or does not extends Simulation"
  val GLOBAL_INFORMATION = "---- Global Information ---"

  def apply(project: Project, script: Script, simulation: Simulation): Reader[Gradle, Observable[Log]] =
    Reader(gradle => {
      val gradleExec = gradle.exec(
        GradleParams(
          project = GradleProject(project.sources),
          task = Task(s"gatling-${script.script}"),
          args = JvmArgs(Map("config" -> createGatlingConfig(simulation.setup.setup, simulation.hosts.hosts)))
        )
      )

      gradleExec.observable
        .flatMap(cancelIfIsScriptError(gradle, gradleExec))
        .subgroupBy(DELIMITER)
        .flatMap(toStringLog)
        .filterNot(isGlobalInformation)
        .filter(isFileOrLog)
        .map(_.trim)
        .flatMap(l => Observable.from(LogParser.parseLog(l)))
    })

  private def cancelIfIsScriptError(gradle: Gradle, gradleExec: Exec[String, GradleCancellationToken]) = {
    (s: String) =>
      if (s.contains(SCRIPT_ERROR)) {
        gradle.cancel(gradleExec.cancelToken)
        Observable.error(new Exception(s))
      }
      else {
        Observable.just(s)
      }
  }

  private def toStringLog =
    (s: (Int, Observable[String])) =>
      s._2.take(100 millis).foldLeft("")((a, s) => s"$a\n$s")

  private def isFileOrLog(s: String): Boolean = s.contains("===") || s.contains("file:")

  private def isGlobalInformation(s: String) = {
    s.contains(GLOBAL_INFORMATION)
  }

  private def id[A]: A => A = a => a

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

  case class Project(sources: String) extends AnyVal

  case class Script(script: String) extends AnyVal
  case class Simulation(hosts: Hosts, setup: Setup)
  case class Hosts(hosts: String*) extends AnyVal

  case class Setup(setup: String) extends AnyVal
}


