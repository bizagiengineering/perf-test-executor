package com.bizagi.gatling.grouper

import rx.lang.scala.Observable

/**
  * Created by dev-williame on 2/11/17.
  */
class ObservableGrouper[T](observable: Observable[T]) {
  def subgroupBy(start: T): Observable[(Int, Observable[T])] = {
    var id = 0
    var state: State = Closed

    def markWith(delimiter: T) =
      (s: T) => {
        val (nextId, next) = groupByDelimiter(delimiter, id, state, s)
        id = nextId
        state = next
        (nextId, s)
      }

    observable
      .map(markWith(start))
      .groupBy(s => s._1, s => s._2)
  }

  private def groupByDelimiter(delimiter: T, id: Int, state: State, value: T) = {
    val next = if (value.equals(delimiter)) state.next else state

    val nextId = state match {
      case Opened => id
      case Closed => id + 1
    }

    (nextId, next)
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
