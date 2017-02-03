
package com.bizagi.gradle

import com.bizagi.gatling.gradle.{Gradle, GradleProject, Task}
import org.scalatest.{FreeSpec, Matchers}

/**
  * Created by feliperojas on 1/26/17.
  */
class GradleTest extends FreeSpec with Matchers {

  "when gradle execution fails return error" - {
    Gradle.apply(
      GradleProject("nonExistingProject"),
      Task("helloWorld")
    ).subscribe(
      onNext = _ => fail(),
      onError = _ => true should be(true),
      onCompleted = () => fail()
    )
  }

  "when gradle execution is correct should get values" - {
    Gradle.apply(
      GradleProject("/Users/dev-williame/dev/RNF/gatling-executor/src/test/resources/gradleTest"),
      Task("helloworld")
    ).subscribe(
      onNext = e => e should not be null,
      onError = _ => fail(),
      onCompleted = () => true should be(true)
    )
  }
}