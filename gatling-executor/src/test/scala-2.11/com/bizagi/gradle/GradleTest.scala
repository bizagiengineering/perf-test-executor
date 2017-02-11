
package com.bizagi.gradle

import java.util.concurrent.TimeUnit

import com.bizagi.gatling.gradle._
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by feliperojas on 1/26/17.
  */
class GradleTest extends FreeSpec with Matchers {

  "when gradle execution fails return error" in {
    Gradle.exec(
      GradleParams(
        GradleProject("nonExistingProject"),
        Task("helloWorld"),
        JvmArgs(Map.empty)
      )
    ).observable.subscribe(
      onNext = _ => fail(),
      onError = _ => {
        true should be(true)
      },
      onCompleted = () => fail()
    )
  }

  "when gradle execution is correct should get values" in {
    Gradle.exec(
      GradleParams(
        GradleProject("/Users/dev-williame/dev/RNF/gatling-executor/src/test/resources/gradleTest"),
        Task("helloworld")
      )).observable.subscribe(
      onNext = e => e should not be null,
      onError = _ => fail(),
      onCompleted = () => true should be(true)
    )
  }

  "Cancel gradle execution" in {
    val execution = Gradle.exec(
      GradleParams(
        GradleProject("/Users/dev-williame/dev/perf-test-executor/gatling-executor/src/test/resources/gradleTest"),
        Task("longHelloWorld")
      )
    )

    val exec = Future {
      execution.observable.subscribe(
        onNext = e => println(e),
        onError = _ => fail(),
        onCompleted = () => true should be(true)
      )
    }

    val cancel = Future {
      TimeUnit.SECONDS.sleep(1)
      Gradle.cancel(execution.cancelToken)
    }

    Await.result(Future.traverse(List(exec, cancel)) { e => e }, Duration.Inf)
  }
}