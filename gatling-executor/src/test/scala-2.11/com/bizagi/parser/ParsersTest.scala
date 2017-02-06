package com.bizagi.parser

import java.time.LocalDateTime

import com.bizagi.gatling.executor._
import com.bizagi.gatling.executor.parser.LogParser
import com.bizagi.gatling.executor.parser.LogParser.PartialParser
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.duration._

/**
  * Created by feliperojas on 2/5/17.
  */
class ParsersTest extends FreeSpec with Matchers {

  "parse boundary" in {
    LogParser.parse(LogParser.boundary, "================================================================================").successful should be(true)
    LogParser.parse(LogParser.boundary, "============================================================================").successful should be(false)
  }

  "parse time" in {
    assertParseResult(
      result = LogParser.parse(PartialParser.time, "2017-02-02 22:20:55                                         105s elapsed"),
      expected = Time(time = LocalDateTime.of(2017, 2, 2, 22, 20, 55), elapsedTime = 105 seconds)
    )
    assertParseResult(
      result = LogParser.parse(PartialParser.time, "2017-02-06 09:54:30                                          45s elapsed"),
      expected = Time(time = LocalDateTime.of(2017, 2, 6, 9, 54, 30), elapsedTime = 45 seconds)
    )
    assertFail(LogParser.parse(PartialParser.time, "2017-02-02 22:2                                         105s elapsed"))
  }

  "parser test simulation" in {
    val result = LogParser.parse(PartialParser.testSimulation,
      """
        |---- TestSimulation ------------------------------------------------------------
        |[########################################################                  ] 76%
        |          waiting: 312    / active: 0      / done: 1008
      """.stripMargin)

    assertParseResult(result, TestSimulation(76, 312, 0, 1008))

    val result2 = LogParser.parse(PartialParser.testSimulation,
      """
        |---- TestSimulation ------------------------------------------------------------
        |[                                                                          ] 76%
        |          waiting: 312    / active: 10      / done: 1008
      """.stripMargin)

    assertParseResult(result2, TestSimulation(76, 312, 10, 1008))

    val result3 = LogParser.parse(PartialParser.testSimulation,
      """
        |---- TestSimulation ------------------------------------------------------------
        |[############                                                              ] 17%
        |          waiting: 1092   / active: 0      / done:228
      """.stripMargin)

    assertParseResult(result3, TestSimulation(17, 1092, 0, 228))
  }

  "parser requests" in {
    val result = LogParser.parse(PartialParser.requests,
      """
        |---- Requests ------------------------------------------------------------------
        |> Global                                                   (OK=1008   KO=0     )
      """.stripMargin)

    assertParseResult(result, Requests(Request(1008, 0), Map.empty))

    val result2 = LogParser.parse(PartialParser.requests,
      """
        |---- Requests ------------------------------------------------------------------
        |> Global                                                   (OK=1008   KO=0     )
        |> Test                                                     (OK=1008   KO=0     )
        |> Login                                                    (OK=100    KO=1     )
      """.stripMargin)

    assertParseResult(result2, Requests(Request(1008, 0), Map("Test" -> Request(1008, 0), "Login" -> Request(100, 1))))
  }

  "parse errors" in {
    val result = LogParser.parse(PartialParser.errors,
      """
        |---- Errors --------------------------------------------------------------------
        |> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:      5 (62.50%)
      """.stripMargin)

    assertParseResult(result, Seq(
      Error("j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:", 5, 62.50))
    )

    val result2 = LogParser.parse(PartialParser.errors,
      """
        |---- Errors --------------------------------------------------------------------
        |> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:      5 (62.50%)
        |> jsonPath($.isAuthenticate).find(0).in(true,True), but actually     18 (58.06%)
        |> status.find.not(500), but actually unexpectedly found 500           2 ( 2.00%)
      """.stripMargin)

    assertParseResult(result2, Seq(
      Error("j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:", 5, 62.50),
      Error("jsonPath($.isAuthenticate).find(0).in(true,True), but actually", 18, 58.06),
      Error("status.find.not(500), but actually unexpectedly found 500", 2, 2.00))
    )

    val result3 = LogParser.parse(PartialParser.errors,
      """
        |---- Errors --------------------------------------------------------------------
        |> jsonPath($.isAuthenticate).find(0).in(true,True), but actually     53 (53.00%)
        | found false
        |> jsonPath($.caseInfo.idCase).find(0).exists failed, could not p     45 (45.00%)
        |repare: Boon failed to parse into a valid AST: Unable to deter...
        |> status.find.not(500), but actually unexpectedly found 500           2 ( 2.00%)
      """.stripMargin)

    assertParseResult(result3, Seq(
      Error("jsonPath($.isAuthenticate).find(0).in(true,True), but actuallyfound false", 53, 53),
      Error("jsonPath($.caseInfo.idCase).find(0).exists failed, could not prepare: Boon failed to parse into a valid AST: Unable to deter...", 45, 45),
      Error("status.find.not(500), but actually unexpectedly found 500", 2, 2.00))
    )

    val result4 = LogParser.parse(PartialParser.errors,
      """
        |---- Errors --------------------------------------------------------------------
        |> j.n.ConnectException: Connection refused: localhost/127.0.0.1:    120 (52.63%)
        |8080
        |> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:    108 (47.37%)
        |0:0:1:8080
      """.stripMargin)

    assertParseResult(result4, Seq(
      Error("j.n.ConnectException: Connection refused: localhost/127.0.0.1:8080", 120, 52.63),
      Error("j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:0:0:1:8080", 108, 47.37))
    )
  }

  "" in {
  }

  private def assertParseResult[A](result: LogParser.ParseResult[A], expected: A): Unit = {
    if (result.successful)
      result.get should be(expected)
    else {
      println(result)
      fail()
    }
  }

  private def assertFail[A](result: LogParser.ParseResult[A]): Unit = {
    if (result.successful)
      fail()
  }
}
