package com.bizagi.script

import java.nio.file.Files

import com.bizagi.gatling.scripts._
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by dev-williame on 1/3/17.
  */
class ScriptManagerTest extends FlatSpec with Matchers {

  "setup" should "clone git repo" in {
    ScriptManager.setup(
      RepoUri("https://bizagidev.visualstudio.com/DefaultCollection/ToolsAndLibs/_git/RNF"),
      RepoCredential(UserName("wfelipe3"), Password("Test")),
      RepoPath(Files.createTempDirectory("git").toString)
    )
      .map(println)
      .unsafePerformIO()
  }

  "upate" should "pull git repo" in {
    ScriptManager.setup(
      RepoUri("https://bizagidev.visualstudio.com/DefaultCollection/ToolsAndLibs/_git/RNF"),
      RepoCredential(UserName("wfelipe3"), Password("Test")),
      RepoPath(Files.createTempDirectory("git").toString)
    )
      .flatMap(ScriptManager.update(RepoCredential(UserName("wfelipe3"), Password("Wfr82636"))))
      .map(println)
      .unsafePerformIO()
  }
}
