package com.bizagi.gatling.executor

import java.time.LocalDateTime

import com.bizagi.gatling.executor.parser.LogParser.PartialParser
import com.bizagi.gatling.gradle.{Gradle, GradleProject, JvmArgs, Task}
import rx.lang.scala.Observable

import scala.concurrent.duration.Duration
import scalaz.Reader
import scala.concurrent.duration._

/**
  * Created by dev-williame on 1/3/17.
  */
object Gatling {

  val DELIMITER: String = "=" * 80

  def apply(project: Project, script: Script, simulation: Simulation): Reader[Gradle, Observable[Log]] =
    Reader(gradle => {
      val gradleObservable = gradle(
        project = GradleProject(project.sources),
        task = Task(s"gatling-${script.script}"),
        args = JvmArgs(Map("config" -> createGatlingConfig(simulation.setup.setup, simulation.hosts.hosts)))
      )

      groupBy(DELIMITER)(gradleObservable)
        .filterNot(_.equals("\n"))
        .groupBy(s => s._1, s => s._2)
        .flatMap(s => s._2.take(10 milli).foldLeft("")((a, s) => s"$a\n$s"))
        .filterNot(_.contains("---- Global Information ---"))
        .filter(s => s.contains("===") || s.contains("file:"))
        .map(_.trim)
        .map(PartialParser.parsePartialLog)
        .map {
          case Left(e) => e
          case Right(p) => p
        }
    })

  private def groupBy(delimiter: String)(observable: Observable[String]) = {
    var id = 0
    var state: State = Closed

    def groupBy(delimiter: String) = {
      s: String =>
        val (nextId, next) = groupByDelimiter(delimiter, id, state, s)
        id = nextId
        state = next
        (nextId, s)
    }

    observable.map(groupBy(DELIMITER))
  }

  private def groupByDelimiter(delimiter: String, id: Int, state: State, value: String) = {
    val next = if (value.equals(delimiter)) state.next else state

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

case class ErrorLog(msg: String) extends Log

case class PartialLog(time: Time,
                      testSimulation: TestSimulation,
                      requests: Requests,
                      errors: Seq[Error]) extends Log

case class Time(time: LocalDateTime, elapsedTime: Duration)

case class UserLog(value: String) extends FinalLog

case class RequestLog(value: String) extends FinalLog

case class TestSimulation(percentage: Int, waiting: Int, active: Int, done: Int)

case class Requests(global: Request, requests: Map[String, Request])

case class Request(ok: Int, ko: Int) {
  def total = ok + ko
}

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

