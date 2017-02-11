package com.bizagi.parser

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import com.bizagi.gatling.gatling.log.Log._
import com.bizagi.gatling.gatling.parser.LogParser
import com.bizagi.gatling.gatling.parser.LogParser.{FileLogParser, LoadLogParser}
import org.scalatest.{FreeSpec, Matchers}

/**
  * Created by dev-williame on 2/6/17.
  */
class GatlingLogParserTest extends FreeSpec with Matchers {

  "Parse start log" in {
    assertParseResult(
      result = LogParser.parse(LoadLogParser.runLog, "RUN     com.bizagi.simulations.TestSimulation           testsimulation  1486558039681           2.0"),
      expected = RunLog(
        simulation = "com.bizagi.simulations.TestSimulation",
        start = toLocalDateTime(1486558039681L)
      )
    )
  }

  "parser end user log" in {
    assertParseResult(
      result = LogParser.parse(LoadLogParser.userLog, "USER    TestSimulation  7       END   1486558044411   1486558044412"),
      expected = UserLog(simulation = "TestSimulation", user = 7, start = toLocalDateTime(1486558044411L), end = toLocalDateTime(1486558044412L))
    )
  }

  "start user log should not be parsed" in {
    LogParser.parse(LoadLogParser.userLog, "USER    TestSimulation  7       START   1486558044411   1486558044411").isEmpty should be(true)
  }

  "parser request log" in {
    assertParseResult(
      result = LogParser.parse(LoadLogParser.requestLog, "REQUEST TestSimulation  6               Test    1486558043909   1486558043913   OK"),
      expected = RequestLog(
        simulation = "TestSimulation",
        user = 6,
        request = "Test",
        start = toLocalDateTime(1486558043909L),
        end = toLocalDateTime(1486558043913L),
        status = OK
      )
    )
  }

  "parser request log with error" in {
    assertParseResult(
      result = LogParser.parse(LoadLogParser.requestLog, "REQUEST TestSimulation  6               Test    1486415973876   1486415973880   KO      j.n.ConnectException: Connection refused: localhost/127.0.0.1:8080"),
      expected = RequestLog(
        simulation = "TestSimulation",
        user = 6,
        request = "Test",
        start = toLocalDateTime(1486415973876L),
        end = toLocalDateTime(1486415973880L),
        status = KO,
        error = Some("j.n.ConnectException: Connection refused: localhost/127.0.0.1:8080")
      )
    )
  }

  "parser parse final logs" in {
    val parsed = Seq(
      "RUN     com.bizagi.simulations.TestSimulation           testsimulation  1486558039681           2.0",
      "USER    TestSimulation  2       START   1486558042060   1486558042060",
      "REQUEST TestSimulation  1               Test    1486558041486   1486558041652   OK",
      "REQUEST TestSimulation  6               Test    1486415973876   1486415973880   KO      j.n.ConnectException: Connection refused: localhost/127.0.0.1:8080",
      "USER    TestSimulation  2       END     1486558042060   1486558042083"
    ).map(LoadLogParser.parseFinalLog)

    parsed should be(
      Seq(
        RunLog(
          simulation = "com.bizagi.simulations.TestSimulation",
          start = toLocalDateTime(1486558039681L)
        ),
        ErrorLog("USER    TestSimulation  2       START   1486558042060   1486558042060", "string matching regex `END' expected but `S' found"),
        RequestLog(
          simulation = "TestSimulation",
          user = 1,
          request = "Test",
          start = toLocalDateTime(1486558041486L),
          end = toLocalDateTime(1486558041652L),
          status = OK
        ),
        RequestLog(
          simulation = "TestSimulation",
          user = 6,
          request = "Test",
          start = toLocalDateTime(1486415973876L),
          end = toLocalDateTime(1486415973880L),
          status = KO,
          error = Some("j.n.ConnectException: Connection refused: localhost/127.0.0.1:8080")
        ),
        UserLog(
          simulation = "TestSimulation",
          user = 2,
          start = toLocalDateTime(1486558042060L),
          end = toLocalDateTime(1486558042083L))
        ))
  }

  "File log parser" in {
    assertParseResult(
      result = LogParser.parse(FileLogParser.fileLog, "Please open the following file: /Users/dev-williame/dev/perf-test-executor/gatling-executor/src/test/resources/simulation.log"),
      expected = List(UserLog(
        simulation = "TestSimulation",
        user = 2,
        start = toLocalDateTime(1486040787490L),
        end = toLocalDateTime(1486040787497L))
      )
    )
  }

  private def toLocalDateTime(date: Long) = {
    LocalDateTime.ofInstant(new Date(date).toInstant, ZoneId.systemDefault())
  }

  private def assertParseResult[A](result: LogParser.ParseResult[A], expected: A): Unit = {
    if (result.successful)
      result.get should be(expected)
    else {
      println(result)
      fail()
    }
  }
}
