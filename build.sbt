name := "perf-test-executor"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "com.offbytwo" % "docopt" % "0.6.0.20150202"
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "com.github.kxbmap" %% "configs" % "0.4.2"
libraryDependencies += "net.liftweb" % "lift-json_2.11" % "3.0-M8"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.9.0.0"

val scalazVersion = "7.3.0-M8"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % scalazVersion,
  "org.scalaz" %% "scalaz-effect" % scalazVersion
)
