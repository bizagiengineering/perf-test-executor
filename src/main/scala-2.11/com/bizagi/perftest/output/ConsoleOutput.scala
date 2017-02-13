package com.bizagi.perftest.output

import com.bizagi.gatling.gatling.log.Log.PartialLog

/**
  * Created by dev-williame on 2/13/17.
  */
object ConsoleOutput {

  def toOutput(partialLog: PartialLog): String = {
      s"""
         |================================================================================
         |${partialLog.time.time}                                         ${partialLog.time.elapsedTime}s elapsed
         |---- TestSimulation ------------------------------------------------------------
         |[########################################################                  ] ${partialLog.testSimulation.percentage}%
         |          waiting: ${partialLog.testSimulation.waiting}    / active: ${partialLog.testSimulation.active}      / done: ${partialLog.testSimulation.done}
         |---- Requests ------------------------------------------------------------------
         |> Global                                                   (OK=${partialLog.requests.global.ok}   KO=${partialLog.requests.global.ko}     )
         |> Test                                                     (OK=1008   KO=0     )
         |---- Errors --------------------------------------------------------------------
         |> jsonPath(  .isAuthenticate).find(0).in(true,True), but actually     53 (53.00%)
         | found false
         |> jsonPath(  .caseInfo.idCase).find(0).exists failed, could not p     45 (45.00%)
         |repare: Boon failed to parse into a valid AST: Unable to deter...
         |> status.find.not(500), but actually unexpectedly found 500           2 ( 2.00%)
         |================================================================================
    """.stripMargin
  }
}
