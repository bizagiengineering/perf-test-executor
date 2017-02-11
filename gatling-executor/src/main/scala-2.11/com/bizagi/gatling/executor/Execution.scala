package com.bizagi.gatling.executor

import rx.lang.scala.Observable

/**
  * Created by dev-williame on 2/10/17.
  */
object Execution {

  def exec[A, B, C](a: A)(implicit executable: Executable[A, B, C]): Exec[B, C] = {
    executable.toExecution(a).exec
  }

  def cancel[A: Cancellable](t: A) = {
    implicitly[Cancellable[A]].toCancellable(t).cancel
  }
}

trait Executable[A, B, C] {
  def toExecution(a: A): Execution[B, C]
}

trait Execution[A, B] {
  def exec: Exec[A, B]
}

case class Exec[A, B](observable: Observable[A], cancelToken: B)

trait Cancellable[T] {
  def toCancellable(t: T): Cancellation
}

trait Cancellation {
  def cancel: Unit
}
