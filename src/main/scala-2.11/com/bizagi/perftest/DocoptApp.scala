package com.bizagi.perftest

import org.docopt.Docopt
import scala.collection.JavaConversions._

/**
  * Created by dev-williame on 2/11/17.
  */
trait DocoptApp {

  def executeWithAppArgs[A](args: Array[String], doc: String, version: String = "1.0")(f: OptsWrapper => A): A = {
    val opts = new Docopt(doc)
      .withVersion(version)
      .withExit(true)
      .parse(args: _*)

    f(new OptsWrapper(opts.toMap))
  }
}

class OptsWrapper(opts: Map[String, AnyRef]) {

  def option[A](f: PartialFunction[String, A]): A = {
    opts
      .filter(isKeyDefindedAt(f))
      .find(isTrue)
      .map(k => f(k._1))
      .getOrElse(f(""))
  }

  def getAs[A](key: String): Option[A] = {
    if (opts.containsKey(key))
      Option(opts(key).asInstanceOf[A])
    else
      None
  }

  def map[A, B](key: String)(f: A => B) = {
    getAs[A](key).map(f)
  }

  def flatmap[A, B](key: String)(f: A => Option[B]) = {
    getAs[A](key).flatMap(f)
  }

  private def isKeyDefindedAt[A](f: PartialFunction[String, A]): ((String, AnyRef)) => Boolean = {
    k => f.isDefinedAt(k._1)
  }

  private def isTrue[A]: ((String, AnyRef)) => Boolean = {
    k => k._2.isInstanceOf[Boolean] && k._2.asInstanceOf[Boolean]
  }
}

