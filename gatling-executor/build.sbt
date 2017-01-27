name := "gatling-executor"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "gradle tooling api" at "https://repo.gradle.org/gradle/libs-releases"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.6.0.201612231935-r"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1"

libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.3.0-M8",
  "org.scalaz" %% "scalaz-effect" % "7.3.0-M8",
  "ch.qos.logback" % "logback-classic" % "1.1.7"
)

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.3"

libraryDependencies += "org.gradle" % "gradle-tooling-api" % "2.14"

libraryDependencies += "io.reactivex" % "rxscala_2.11" % "0.26.5"




    