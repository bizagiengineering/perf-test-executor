package com.bizagi.gatling.gradle

import java.io.{File, OutputStream}

import com.bizagi.gatling.executor._
import com.bizagi.gatling.gatling._
import org.gradle.tooling.{CancellationTokenSource, GradleConnector}
import rx.lang.scala.{Observable, Subscriber}

import scala.util.Try

/**
  * Created by dev-williame on 1/9/17.
  */
trait Gradle {
  def exec(gradleParams: GradleParams): Exec[String, GradleCancellationToken]

  def cancel(gradleCancellationToken: GradleCancellationToken)
}

object Gradle extends Gradle {

  implicit object GradleCancellable extends Cancellable[GradleCancellationToken] {
    override def toCancellable(t: GradleCancellationToken): Cancellation = new Cancellation {
      override def cancel: Unit = t.cancellationTokenSource.cancel()
    }
  }

  implicit object GradleExec extends Executable[GradleParams, String, GradleCancellationToken] {
    override def toExecution(a: GradleParams): Execution[String, GradleCancellationToken] = new Execution[String, GradleCancellationToken] {
      override def exec: Exec[String, GradleCancellationToken] = {
        val cancellationToken = GradleConnector.newCancellationTokenSource()
        val observable = Observable[String] { subscriber =>
          Try {
            val connector = GradleConnector.newConnector()
            connector.forProjectDirectory(new File(a.project.project))
            val connection = connector.connect()
            val launcher = connection.newBuild()
            launcher.forTasks(a.task.task)
            launcher.setStandardError(new StreamableOutputStream(subscriber))
            launcher.setStandardOutput(new StreamableOutputStream(subscriber))
            launcher.setJvmArguments(a.args.args.toSeq.map(toJvmArgument): _*)
            launcher.withCancellationToken(cancellationToken.token())
            launcher.run()
            subscriber.onCompleted()
          }.recover {
            case e => subscriber.onError(e)
          }
        }
        Exec(observable, GradleCancellationToken(cancellationToken))
      }
    }
  }

  private def toJvmArgument(kv: (String, String)) = s"-D${kv._1}=${kv._2}"

  def exec(gradleParams: GradleParams): Exec[String, GradleCancellationToken] =
    Execution.exec(gradleParams)

  def cancel(gradleCancellationToken: GradleCancellationToken) =
    Execution.cancel(gradleCancellationToken)
}

case class GradleCancellationToken(cancellationTokenSource: CancellationTokenSource)

case class GradleParams(project: GradleProject, task: Task, args: JvmArgs = JvmArgs(Map.empty))

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
