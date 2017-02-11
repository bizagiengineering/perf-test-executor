package com.bizagi.gatling.gatling.log

import java.time.LocalDateTime

import scala.concurrent.duration.Duration

/**
  * Created by dev-williame on 2/11/17.
  */
object Log {
  trait Log
  trait FinalLog extends Log

  case class ErrorLog(log: String, msg: String) extends Log

  case class PartialLog(time: Time,
                        testSimulation: TestSimulation,
                        requests: Requests,
                        errors: Seq[Error]) extends Log

  case class Time(time: LocalDateTime, elapsedTime: Duration)

  case class RunLog(simulation: String, start: LocalDateTime) extends FinalLog

  case class UserLog(simulation: String, user: Int, start: LocalDateTime, end: LocalDateTime) extends FinalLog

  case class RequestLog(simulation: String, user: Int, request: String, start: LocalDateTime, end: LocalDateTime, status: Status, error: Option[String] = None) extends FinalLog

  trait Status
  object OK extends Status
  object KO extends Status

  case class TestSimulation(percentage: Int, waiting: Int, active: Int, done: Int)

  case class Requests(global: Request, requests: Map[String, Request])

  case class Request(ok: Int, ko: Int) {
    def total = ok + ko
  }

  case class Error(message: String, quantity: Int, percentage: Double)

}


