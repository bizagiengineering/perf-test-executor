package com.bizagi.gatling.gradle

import akka.stream.scaladsl.Source
import akka.stream._
import akka.stream.javadsl.StreamConverters
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import com.bizagi.gatling.executor.Exec
import com.bizagi.gatling.gatling.Gatling.GatlingExec

/**
  * Created by dev-williame on 2/20/17.
  */
object Gradle2 {

  def exec(gradleParams: GradleParams): Exec[String, GradleCancellationToken] = {
    StreamConverters.fromOutputStream()
    Source()
  }

  class GradleGraph extends GraphStage[SourceShape[String]] {
    val out: Outlet[String] = Outlet("logs")
    override def shape: SourceShape[String] = SourceShape(out)
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            push(out, "")
          }
        })
      }
  }
}
