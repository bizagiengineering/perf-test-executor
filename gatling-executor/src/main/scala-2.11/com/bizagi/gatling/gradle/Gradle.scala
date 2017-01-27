package com.bizagi.gatling.gradle

import java.io.{File, OutputStream}

import org.gradle.tooling.GradleConnector
import rx.lang.scala.{Observable, Subscriber}

import scala.util.Try
import scalaz.effect.IO

/**
  * Created by dev-williame on 1/9/17.
  */
object Gradle {

  def execute(project: GradleProject, task: Task, args: JvmArgs = JvmArgs(Map.empty)): Observable[String] =
    Observable { subscriber =>
      Try {
        val connector = GradleConnector.newConnector()
        connector.forProjectDirectory(new File(project.project))
        val connection = connector.connect()
        val launcher = connection.newBuild()
        launcher.forTasks(task.task)
        launcher.setStandardError(new StreamableOutputStream(subscriber))
        launcher.setStandardOutput(new StreamableOutputStream(subscriber))
        launcher.setJvmArguments(args.args.toSeq.map(toJvmArgument): _*)
        launcher.run()
        subscriber.onCompleted()
      }.recover {
        case e => subscriber.onError(e)
      }
    }

  private def toJvmArgument(kv: (String, String)) = s"-D${kv._1}=${kv._2}"
}

case class GradleProject(project: String) extends AnyVal

case class Task(task: String) extends AnyVal

case class JvmArgs(args: Map[String, String]) extends AnyVal


class StreamableOutputStream(val subscriber: Subscriber[String]) extends OutputStream {

  private var value = Array[Byte]()

  override def close(): Unit = ()

  override def flush(): Unit = {
    subscriber.onNext(new String(value))
    value = Array[Byte]()
  }

  override def write(b: Array[Byte]): Unit = {
    value = value ++ b
  }

  override def write(b: Int): Unit = {
    value = value :+ b.toByte
  }
}
