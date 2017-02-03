package com.bizagi.parser

import java.time.LocalDateTime

import com.bizagi.gatling.executor._
import com.bizagi.gatling.executor.parser.LogParser.PartialParser
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.duration._

/**
  * Created by dev-williame on 2/2/17.
  */
class PartialLogParserTest extends FreeSpec with Matchers {

  "given error string then return error" - {
    PartialParser.parsePartialLog("") should be(Left("string matching regex `================================================================================' expected but end of source found"))
  }

  "given partial log without errors then return partial log" - {
    val log =
      """
        |================================================================================
        |2017-02-02 22:20:55                                         105s elapsed
        |---- TestSimulation ------------------------------------------------------------
        |[########################################################                  ] 76%
        |          waiting: 312    / active: 0      / done: 1008
        |---- Requests ------------------------------------------------------------------
        |> Global                                                   (OK=1008   KO=0     )
        |> Test                                                     (OK=1008   KO=0     )
        |================================================================================
      """.stripMargin

    PartialParser.parsePartialLog(log) should be(Right(PartialLog(
      time = Time(time = LocalDateTime.of(2017, 2, 2, 22, 20, 55), elapsedTime = 105 seconds),
      testSimulation = TestSimulation(percentage = 76, waiting = 312, active = 0, done = 1008),
      requests = Requests(Request(1008, 0), Map("Test" -> Request(1008, 0))),
      errors = Seq.empty
    )))
  }

  "given partial log with errors then return partial log with errors" - {
    val log =
      """
        |================================================================================
        |2017-02-02 22:20:55                                         105s elapsed
        |---- TestSimulation ------------------------------------------------------------
        |[########################################################                  ] 76%
        |          waiting: 312    / active: 0      / done: 1008
        |---- Requests ------------------------------------------------------------------
        |> Global                                                   (OK=1008   KO=0     )
        |> Test                                                     (OK=1008   KO=0     )
        |---- Errors --------------------------------------------------------------------
        |> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:      5 (62.50%)
        |> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:     18 (45.00%)
        |================================================================================
      """.stripMargin

    PartialParser.parsePartialLog(log) should be(Right(PartialLog(
      time = Time(time = LocalDateTime.of(2017, 2, 2, 22, 20, 55), elapsedTime = 105 seconds),
      testSimulation = TestSimulation(percentage = 76, waiting = 312, active = 0, done = 1008),
      requests = Requests(Request(1008, 0), Map("Test" -> Request(1008, 0))),
      errors = Seq(
        Error("j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:", 5, 62.50),
        Error("j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:", 18, 45))
    )))
  }
}
