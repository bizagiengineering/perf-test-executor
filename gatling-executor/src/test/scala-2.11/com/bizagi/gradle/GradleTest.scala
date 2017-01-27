package com.bizagi.gradle

import com.bizagi.gatling.gradle.{Gradle, GradleProject, Task}
import org.scalatest.{FreeSpec, Matchers}

/**
  * Created by feliperojas on 1/26/17.
  */
class GradleTest extends FreeSpec with Matchers {

  "when gradle execution fails return error" - {
    Gradle.execute(
      GradleProject("nonExistingProject"),
      Task("helloWorld")
    ).map { o =>
      o.subscribe(
        onNext = _ => (),
        onError = _ => {
          true should be(true)
        },
        onCompleted = () => {
          fail()
        }
      )
    }.unsafePerformIO()
  }

  "when gradle execution is correct should get values" - {
    Gradle.execute(
      GradleProject("/Users/feliperojas/git/gatling-executor/src/test/resources/gradleTest"),
      Task("helloworld")
    ).map { o =>
      o.filterNot(_.equals("\n")).
        subscribe(
          onNext = e => {
            e should not be null
          },
          onError = _ => fail(),
          onCompleted = () => {
            true should be(true)
          }
        )
    }.unsafePerformIO()
  }

}
