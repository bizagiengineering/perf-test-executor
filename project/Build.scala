import sbt._
import Keys._

object PerfTestToolBuild extends Build {
  lazy val gatlingExecutor = Project(id = "gatling-executor", base = file("gatling-executor"))
  lazy val root = Project(id = "root", base = file(".")) aggregate gatlingExecutor dependsOn gatlingExecutor
}
