package com.bizagi.gatling

import com.bizagi.gatling.executor._
import org.scalatest.{FreeSpec, Matchers}

/**
  * Created by dev-williame on 1/3/17.
  */
class GatlingTest extends FreeSpec with Matchers {

  "gatling executor should return error message when it fails" - {
    Gatling.execute(
      Project("/Users/dev-williame/dev/RNF/scenarios/gatling-gradle"),
      Script("com.bizagi.simulations.TestSimulation"),
      Simulation(Hosts("http://localhost"),
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
        """.stripMargin)
    ).subscribe(onNext = l => println(l))
  }
}