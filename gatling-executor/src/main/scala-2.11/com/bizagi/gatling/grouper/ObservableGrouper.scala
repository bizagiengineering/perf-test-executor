package com.bizagi.gatling.grouper

import rx.lang.scala.Observable

/**
  * Created by dev-williame on 2/11/17.
  */
class ObservableGrouper[T](observable: Observable[T]) {

  def subgroupBy(zero: T, start: T): Observable[(Int, Observable[T])] = {
    observable.scan((0, Closed: State, zero)) { (s, v) =>
      val (id, state, _) = s
      markSameDelimiter(start, id, state, v)
    }.groupBy(s => s._1, s => s._3)
  }

  private def markSameDelimiter(delimiter: T, id: Int, state: State, value: T) = {
    val next = if (value.equals(delimiter)) state.next else state

    val nextId = state match {
      case Opened => id
      case Closed => id + 1
    }

    (nextId, next, value)
  }
}
object ObservableGrouper {
  implicit def toGrouper[T](o: Observable[T]): ObservableGrouper[T] = {
    new ObservableGrouper[T](o)
  }
}

trait State {
  def next: State
}

object Opened extends State {
  override def next = Closed
}

object Closed extends State {
  override def next = Opened
}
