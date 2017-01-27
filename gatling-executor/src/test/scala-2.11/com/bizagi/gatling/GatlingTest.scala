package com.bizagi.gatling

import com.bizagi.gatling.executor._
import org.scalatest.{FreeSpec, Matchers}

/**
  * Created by dev-williame on 1/3/17.
  */
class GatlingTest extends FreeSpec with Matchers {

  "gatling executor should return error message when it fails" - {
    Gatling.execute(
      Project("/Users/feliperojas/bizagi/rnf/scenarios/gatling-gradle"),
      Script("com.bizagi.simulations.TestSimulation"),
      Simulation(Hosts("localhost"), "")
    )
  }

  //  "gatling executor" should "execute gatling load test" in {
  //    Gatling.execute(
  //      gradle.Project("/Users/dev-williame/dev/RNF/scenarios/gatling-gradle"),
  //      Script("com.bizagi.simulations.TestSimulation"),
  //      Hosts(Seq("localhost"))
  //    )
  //      .map(o => println(o))
  //      .unsafePerformIO()
  //  }
}
