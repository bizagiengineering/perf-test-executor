package com.bizagi.gatling.executor

import java.time.LocalDateTime

import com.bizagi.gatling.gradle.{Gradle, GradleProject, JvmArgs, Task}
import rx.lang.scala.Observable

import scala.concurrent.duration.Duration
import scalaz.Reader
import scala.concurrent.duration._

/**
  * Created by dev-williame on 1/3/17.
  */
object Gatling {

  val DELIMITER = "================================================================================"

  def apply(project: Project, script: Script, simulation: Simulation): Reader[Gradle, Observable[Log]] = {
    var id = 0
    var state: State = Closed
    Reader(gradle => {
      gradle(
        GradleProject(project.sources),
        Task(s"gatling-${script.script}"),
        JvmArgs(Map("config" -> createGatlingConfig(simulation.setup.setup, simulation.hosts.hosts)))
      ).filterNot(_.equals("\n"))
        .map { s =>
          val (nextId, next) = groupByDelimiter(id, state, s)
          id = nextId
          state = next
          (nextId, s)
        }
        .groupBy(s => s._1, s => s._2)
        .flatMap(s => s._2.take(10 milli).foldLeft("")((a, s) => s"$a\n$s"))
        .filter(s => s.contains("===") || s.contains("file:")).map(l => UserLog(l))
    })
  }

  private def groupByDelimiter(id: Int, state: State, value: String) = {
    val next = if (value.equals(DELIMITER)) state.next else state

    val nextId = state match {
      case Opened => id
      case Closed => id + 1
    }

    (nextId, next)
  }

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

trait Log

trait FinalLog extends Log

case class PartialLog(time: Time,
                      testSimulation: TestSimulation,
                      requests: Requests,
                      errors: Seq[Error]) extends Log

case class Time(time: LocalDateTime, elapsedTime: Duration)

case class UserLog(value: String) extends FinalLog

case class RequestLog(value: String) extends FinalLog

case class TestSimulation(percentage: Int, waiting: Int, active: Int, done: Int)

case class Requests(global: Request, requests: Map[String, Request])

case class Request(ok: Int, ko: Int)

case class Error(message: String, quantity: Int, percentage: Double)

case class Hosts(hosts: String*) extends AnyVal

case class Setup(setup: String) extends AnyVal

case class Simulation(hosts: Hosts, setup: Setup)

trait State {
  def next: State
}

object Opened extends State {
  override def next = Closed
}

object Closed extends State {
  override def next = Opened
}

