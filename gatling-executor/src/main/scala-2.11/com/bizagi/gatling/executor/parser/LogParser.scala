package com.bizagi.gatling.executor.parser

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.bizagi.gatling.executor._

import scala.concurrent.duration.Duration
import scala.util.parsing.combinator._
import scala.concurrent.duration._

/**
  * Created by dev-williame on 2/2/17.
  */
object LogParser {

  object PartialParser extends RegexParsers {

    def boundary: Parser[String] =
      """================================================================================""".r
        .^^(_ => "")

    def time: Parser[Time] = {
      val date: Parser[LocalDateTime] =
        "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}".r ^^ (s => LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))

      val duration: Parser[Duration] = "\\d+s".r ^^ (s => {
        s.replace("s", "").toInt seconds
      })
      val elapsed: Parser[String] = "elapsed".r ^^ (_ => "")
      val elapsedTime = duration ~ elapsed ^^ { case d ~ _ => d }

      date ~ elapsedTime ^^ { case d ~ time => Time(d, time) }
    }

    def testSimulation: Parser[TestSimulation] = {
      val header: Parser[String] =
        "---- TestSimulation ------------------------------------------------------------"
          .r ^^ (_ => "")

      val loadBar: Parser[String] = "\\[#*\\s*\\]".r ^^ (_ => "")

      val percentage: Parser[Int] = "\\d+%".r ^^ (s => s.replace("%", "").toInt)

      val slash: Parser[Unit] = "/".r ^^ (_ => ())
      val int: Parser[Int] = "\\d+".r ^^ (_.toInt)
      val wait: Parser[Int] = ("waiting:".r ^^ (_ => "")) ~ int ^^ { case _ ~ i => i }
      val active: Parser[Int] = ("active:".r ^^ (_ => "")) ~ int ^^ { case _ ~ i => i }
      val done: Parser[Int] = ("done:".r ^^ (_ => "")) ~ int ^^ { case _ ~ i => i }

      header ~ loadBar ~ percentage ~ wait ~ slash ~ active ~ slash ~ done ^^ { case _ ~ _ ~ p ~ w ~ _ ~ a ~ _ ~ d => TestSimulation(p, w, a, d) }
    }

    def requests: Parser[Requests] = {
      val header: Parser[Unit] =
        "---- Requests ------------------------------------------------------------------"
          .r ^^ (_ => ())

      val int: Parser[Int] = "\\d+".r ^^ (_.toInt)
      val start: Parser[Unit] = ">".r ^^ (_ => ())
      val name: Parser[String] = "\\w+".r ^^ (s => s)
      val ok: Parser[Unit] = "\\(OK=".r ^^ (_ => ())
      val ko: Parser[Unit] = "KO=".r ^^ (_ => ())
      val close: Parser[Unit] = "\\)".r ^^ (_ => ())

      val row: Parser[(String, Request)] =
        start ~ name ~ ok ~ int ~ ko ~ int ~ close ^^ { case _ ~ n ~ _ ~ o ~ _ ~ k ~ _ => (n, Request(o, k)) }

      val global = row.map(_._2)

      val rows = row.* ^^ (l => l.groupBy(_._1).map(s => s._2.head))

      header ~ global ~ rows ^^ { case _ ~ g ~ r => Requests(g, r) }
    }

    def errors: Parser[Seq[com.bizagi.gatling.executor.Error]] = {
      val header: Parser[Unit] =
        "---- Errors --------------------------------------------------------------------".r ^^ (_ => ())

      val start: Parser[Unit] = ">".r ^^ (_ => ())
      val name: Parser[String] = "\\.+".r ^^ (s => s)
      val int: Parser[Int] = "\\d+".r ^^ (_.toInt)
      val percentage: Parser[Double] = "\\(d+\\.?\\d+%\\)".r ^^ (s => s.replace("(", "").replace("%)", "").toDouble)

      val row = start ~ name ~ int ~ percentage ~ (name ?) ^^ { case _ ~ n1 ~ i ~ p ~ n2 => com.bizagi.gatling.executor.Error(s"$n1$n2", i, p) }
      val rows = row +

      header ~ rows ^^ { case _ ~ e => e }
    }

    def partialLog: Parser[PartialLog] =
      boundary ~ time ~ testSimulation ~ requests ~ (errors ?) ~ boundary ^^ {
        case _ ~ t ~ ts ~ r ~ e ~ _ => PartialLog(t, ts, r, e.getOrElse(Seq.empty))
      }

    def parsePartialLog(log: String): Either[String, PartialLog] =
      parse(partialLog, log) match {
        case Success(matched, _) => Right(matched)
        case Failure(msg, _) => Left(msg)
        case Error(msg, _) => Left(msg)
      }
  }

}
