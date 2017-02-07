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
object LogParser extends RegexParsers {

  val int: Parser[Int] = "\\d+".r ^^ (_.toInt)

  def boundary: Parser[Unit] =
    "={80}".r ^^ (_ => ())

  object PartialParser {

    def time: Parser[Time] = {
      val date: Parser[LocalDateTime] =
        "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}".r ^^ (s => LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))

      val duration: Parser[Duration] = "\\d+".r <~ "s elapsed".r ^^ (_.toInt seconds)

      date ~ duration ^^ { case d ~ time => Time(d, time) }
    }

    def testSimulation: Parser[TestSimulation] = {
      val header: Parser[Unit] = "-{4} TestSimulation -+".r ^^ (_ => ())

      val loadBar: Parser[Unit] = "\\[#*-*\\s*\\]".r ^^ (_ => ())

      val percentage: Parser[Int] = "\\d+".r <~ "%".r ^^ (_.toInt)

      val slash: Parser[Unit] = "/".r ^^ (_ => ())
      val wait: Parser[Int] = ("waiting:".r ^^ (_ => "")) ~> int <~ slash
      val active: Parser[Int] = ("active:".r ^^ (_ => "")) ~> int <~ slash
      val done: Parser[Int] = ("done:".r ^^ (_ => "")) ~> int

      header ~> loadBar ~> percentage ~ wait ~ active ~ done ^^ { case p ~ w ~ a ~ d => TestSimulation(p, w, a, d) }
    }

    def requests: Parser[Requests] = {
      val header: Parser[Unit] = "-{4} Requests -+".r ^^ (_ => ())

      val start: Parser[Unit] = ">".r ^^ (_ => ())
      val name: Parser[String] = "\\w+".r ^^ (s => s)
      val ok: Parser[Int] = ("\\(OK=".r ^^ (_ => ())) ~> int
      val ko: Parser[Int] = ("KO=".r ^^ (_ => ())) ~> int
      val close: Parser[Unit] = "\\)".r ^^ (_ => ())

      val row: Parser[(String, Request)] =
        start ~> name ~ ok ~ ko <~ close ^^ { case n ~ o ~ k => (n, Request(o, k)) }

      val global = row.map(_._2)

      val rows = row.* ^^ (l => l.groupBy(_._1).map(_._2.head))

      header ~> global ~ rows ^^ { case g ~ r => Requests(g, r) }
    }

    def errors: Parser[Seq[com.bizagi.gatling.executor.Error]] = {
      val header: Parser[Unit] =
        "-{4} Errors -+".r ^^ (_ => ())

      val start: Parser[Unit] = ">".r ^^ (_ => ())
      val error: Parser[String] = ".+ {2,}".r ^^ (_.trim)
      val percentage: Parser[Double] = "\\(".r ~> "\\d+\\.?\\d+".r <~ "%\\)".r ^^ (_.toDouble)
      val errorCont: Parser[String] = "[^>=].+\n".r ^^ (_.trim)

      val row = start ~> error ~ int ~ percentage ~ (errorCont ?) ^^ {
        case n1 ~ i ~ p ~ e => com.bizagi.gatling.executor.Error(s"$n1${e.getOrElse("")}".trim, i, p)
      }

      header ~> (row +)
    }

    def partialLog: Parser[PartialLog] =
      boundary ~> time ~ testSimulation ~ requests ~ (errors ?) <~ boundary ^^ {
        case t ~ ts ~ r ~ e => PartialLog(t, ts, r, e.getOrElse(Seq.empty))
      }

    def parsePartialLog(log: String): Either[ErrorLog, PartialLog] =
      parse(partialLog, log) match {
        case Success(matched, _) => Right(matched)
        case Failure(msg, _) => Left(ErrorLog(log, msg))
        case Error(msg, _) => Left(ErrorLog(log, msg))
      }
  }

}
