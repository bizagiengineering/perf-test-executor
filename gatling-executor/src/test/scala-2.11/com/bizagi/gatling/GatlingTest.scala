package com.bizagi.gatling

import java.time.LocalDateTime

import com.bizagi.gatling.executor._
import com.bizagi.gatling.gradle.{Gradle, GradleProject, JvmArgs, Task}
import org.scalatest.{FreeSpec, Matchers}
import rx.lang.scala.Observable
import scala.concurrent.duration._

/**
  * Created by dev-williame on 1/3/17.
  */
class GatlingTest extends FreeSpec with Matchers {

  val setup: String =
    """
      | {
      |     "rampUsersPerSec": {
      |        "users": 2,
      |        "to": 20
      |        "during": {
      |            "time": 2,
      |            "unit": "MINUTES"
      |        },
      |        "randomized": false
      |     }
      | }
    """.stripMargin

  "gatling executor should return error when the execution fails" in {
    val exception = new RuntimeException("test error")

    val gradleMock = new Gradle {
      override def apply(project: GradleProject, task: Task, args: JvmArgs): Observable[String] = {
        Observable.error(exception)
      }
    }

    Gatling.apply(Project(""), Script(""), Simulation(Hosts(""), Setup(""))).run(gradleMock)
      .subscribe(onNext = _ => fail(), onError = t => t should be(exception), onCompleted = () => fail())
  }

  "gatling executor should return intermediate events" in {
    val gradleMock = new Gradle {
      override def apply(project: GradleProject, task: Task, args: JvmArgs): Observable[String] =
        Observable.from(
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
          """.stripMargin.split("\n")
        )
    }

    val gatling = Gatling(
      Project("/Users/dev-williame/dev/RNF/scenarios/gatling-gradle"),
      Script("com.bizagi.simulations.TestSimulation"),
      Simulation(Hosts("http://localhost:8080"), Setup(setup))
    )

    gatling.run(gradleMock)
      .subscribe(_ should be(PartialLog(
        time = Time(LocalDateTime.of(2017, 2, 2, 22, 20, 55), elapsedTime = 105 seconds),
        testSimulation = TestSimulation(percentage = 76, waiting = 312, active = 0, done = 1008),
        requests = Requests(Request(1008, 0), Map("Test" -> Request(1008, 0))),
        errors = Seq.empty
      )))
  }

  "gatling executor should return intermediate events with errors" in {
    val gradleMock = new Gradle {
      override def apply(project: GradleProject, task: Task, args: JvmArgs): Observable[String] =
        Observable.from(
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
            |> jsonPath($.isAuthenticate).find(0).in(true,True), but actually     53 (53.00%)
            | found false
            |> jsonPath($.caseInfo.idCase).find(0).exists failed, could not p     45 (45.00%)
            |repare: Boon failed to parse into a valid AST: Unable to deter...
            |> status.find.not(500), but actually unexpectedly found 500           2 ( 2.00%)
            |================================================================================
          """.stripMargin.split("\n")
        )
    }

    val gatling = Gatling(
      Project("/Users/dev-williame/dev/RNF/scenarios/gatling-gradle"),
      Script("com.bizagi.simulations.TestSimulation"),
      Simulation(Hosts("http://localhost:8080"), Setup(setup))
    )

    gatling.run(gradleMock)
      .subscribe(_ should be(PartialLog(
        time = Time(LocalDateTime.of(2017, 2, 2, 22, 20, 55), elapsedTime = 105 seconds),
        testSimulation = TestSimulation(percentage = 76, waiting = 312, active = 0, done = 1008),
        requests = Requests(Request(1008, 0), Map("Test" -> Request(1008, 0))),
        errors = Seq(
          Error("jsonPath($.isAuthenticate).find(0).in(true,True), but actuallyfound false", 53, 53),
          Error("jsonPath($.caseInfo.idCase).find(0).exists failed, could not prepare: Boon failed to parse into a valid AST: Unable to deter...", 45, 45),
          Error("status.find.not(500), but actually unexpectedly found 500", 2, 2.00))
      )))
  }

  "gatling real" in {
    val gradleMock = new Gradle {
      override def apply(project: GradleProject, task: Task, args: JvmArgs): Observable[String] =
        Observable.from(Seq(
          "================================================================================",
          "2017-02-02 08:06:30                                           5s elapsed",
          "---- TestSimulation ------------------------------------------------------------",
          "[                                                                          ]  0%",
          "          waiting: 1312   / active: 0      / done:8     ",
          "---- Requests ------------------------------------------------------------------",
          "> Global                                                   (OK=0      KO=8     )",
          "> Test                                                     (OK=0      KO=8     )",
          "---- Errors --------------------------------------------------------------------",
          "> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:      5 (62.50%)",
          "> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:     18 (45.00%)",
          "================================================================================",
          "================================================================================",
          "2017-02-02 08:06:30                                           5s elapsed",
          "---- TestSimulation ------------------------------------------------------------",
          "[                                                                          ]  0%",
          "          waiting: 1312   / active: 0      / done:8     ",
          "---- Requests ------------------------------------------------------------------",
          "> Global                                                   (OK=0      KO=8     )",
          "> Test                                                     (OK=0      KO=8     )",
          "---- Errors --------------------------------------------------------------------",
          "> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:      5 (62.50%)",
          "> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:     18 (45.00%)",
          "================================================================================",
          "Simulation com.bizagi.simulations.TestSimulation completed in 120 seconds",
          "Parsing log file(s)...",
          "Parsing log file(s) done",
          "Generating reports...",
          "================================================================================",
          "---- Global Information --------------------------------------------------------",
          "> request count                                       1320 (OK=0      KO=1320  )",
          "> min response time                                      0 (OK=-      KO=0     )",
          "> max response time                                     69 (OK=-      KO=69    )",
          "> mean response time                                     1 (OK=-      KO=1     )",
          "> std deviation                                          2 (OK=-      KO=2     )",
          "> response time 50th percentile                          1 (OK=-      KO=1     )",
          "> response time 75th percentile                          2 (OK=-      KO=2     )",
          "> response time 95th percentile                          5 (OK=-      KO=5     )",
          "> response time 99th percentile                         10 (OK=-      KO=10    )",
          "> mean requests/sec                                     11 (OK=-      KO=11    )",
          "---- Response Time Distribution ------------------------------------------------",
          "> t < 800 ms                                             0 (  0%)",
          "> 800 ms < t < 1200 ms                                   0 (  0%)",
          "> t > 1200 ms                                            0 (  0%)",
          "> failed                                              1320 (100%)",
          "---- Errors --------------------------------------------------------------------",
          "> j.n.ConnectException: Connection refused: localhost/0:0:0:0:0:    712 (53.94%)",
          "0:0:1:80                                                                        ",
          "> j.n.ConnectException: Connection refused: localhost/127.0.0.1:    608 (46.06%)",
          "80",
          "================================================================================",
          "Reports generated in 0s.",
          "Please open the following file: /Users/dev-williame/dev/RNF/scenarios/gatling-gradle/build/reports/gatling/testsimulation-1486040785206/index.html",
          "BUILD SUCCESSFUL",
          "Total time: 2 mins 16.842 secs"
        ))
    }

    Gatling(
      Project("/Users/dev-williame/dev/RNF/scenarios/gatling-gradle"),
      Script("com.bizagi.simulations.TestSimulation"),
      Simulation(Hosts("http://localhost:8080"), Setup(setup))
    ).run(Gradle).foreach(println)
  }
}