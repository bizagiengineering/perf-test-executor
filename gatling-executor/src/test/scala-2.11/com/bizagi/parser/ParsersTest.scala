package com.bizagi.parser

import java.time.LocalDateTime

import com.bizagi.gatling.executor.{Request, Requests, TestSimulation, Time}
import com.bizagi.gatling.executor.parser.LogParser
import com.bizagi.gatling.executor.parser.LogParser.PartialParser
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.duration._

/**
  * Created by feliperojas on 2/5/17.
  */
class ParsersTest extends FreeSpec with Matchers {

  "parse boundary" - {
    LogParser.parse(PartialParser.boundary, "================================================================================").successful should be(true)
    LogParser.parse(PartialParser.boundary, "============================================================================").successful should be(false)
  }

  "parse time" - {
    assertParseResult(
      result = LogParser.parse(PartialParser.time, "2017-02-02 22:20:55                                         105s elapsed"),
      expected = Time(time = LocalDateTime.of(2017, 2, 2, 22, 20, 55), elapsedTime = 105 seconds)
    )
    assertFail(LogParser.parse(PartialParser.time, "2017-02-02 22:2                                         105s elapsed"))
  }

  "parser test simulation" - {
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
  }

  "parser requests" - {
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
        |> Login                                                    (OK=100    KO=1      )
      """.stripMargin)

    assertParseResult(result2, Requests(Request(1008, 0), Map("Test" -> Request(1008, 0), "Login" -> Request(100, 1))))
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
