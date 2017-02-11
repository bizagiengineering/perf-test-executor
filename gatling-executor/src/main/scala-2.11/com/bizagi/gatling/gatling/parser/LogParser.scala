package com.bizagi.gatling.gatling.parser

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import com.bizagi.gatling.gatling.log.Log._

import scala.concurrent.duration.{Duration, _}
import scala.util.Try
import scala.util.parsing.combinator._

/**
  * Created by dev-williame on 2/2/17.
  */
object LogParser extends RegexParsers {

  val int: Parser[Int] = "\\d+".r ^^ (_.toInt)

  def boundary: Parser[Unit] =
    "={80}".r ^^ (_ => ())

  private def toLocalDateTime(date: Long) = {
    LocalDateTime.ofInstant(new Date(date).toInstant, ZoneId.systemDefault())
  }

  object FileLogParser {
    def fileLog: Parser[Iterable[Log]] = {
      val text: Parser[String] = "Please open the following file: ".r
      val file: Parser[Iterable[Log]] = (".*".r ^^ (_.replace("index.html", "simulation.log")))
        .map { f =>
          Try(scala.io.Source.fromFile(f).getLines().toIterable
            .filterNot(s => s.contains("USER") && s.contains("START"))
          )
        }
        .map { v =>
          v.map(i => i.map(LoadLogParser.parseFinalLog))
            .recover {
              case e: Exception => List(ErrorLog("", e.getMessage))
            }.get
        }

      text ~> file
    }
  }

  object LoadLogParser {
    def runLog: Parser[RunLog] = {
      val run: Parser[String] = "RUN".r
      val simulationName: Parser[String] = "[\\w\\.]+".r
      val name: Parser[String] = "\\w+".r
      val time: Parser[LocalDateTime] = "\\d+".r ^^ (s => toLocalDateTime(s.toLong))
      val version: Parser[Double] = "\\d+\\.\\d+".r ^^ (_.toDouble)

      run ~> simulationName ~ name ~ time <~ version ^^ {
        case s ~ _ ~ t => RunLog(s, t)
      }
    }

    def userLog: Parser[UserLog] = {
      val user: Parser[String] = "USER".r
      val simulation: Parser[String] = "\\w+".r
      val userId: Parser[Int] = "\\d+".r ^^ (_.toInt)
      val state: Parser[String] = "END".r
      val start: Parser[LocalDateTime] = "\\d+".r ^^ (s => toLocalDateTime(s.toLong))
      val end: Parser[LocalDateTime] = "\\d+".r ^^ (s => toLocalDateTime(s.toLong))

      user ~> simulation ~ userId ~ state ~ start ~ end ^^ {
        case s ~ id ~ _ ~ st ~ e => UserLog(s, id, st, e)
      }
    }

    def requestLog: Parser[RequestLog] = {
      val request: Parser[String] = "REQUEST".r
      val name: Parser[String] = "\\w+".r
      val userId: Parser[Int] = "\\d+".r ^^ (_.toInt)
      val requestName: Parser[String] = "\\w+".r
      val start: Parser[LocalDateTime] = "\\d+".r ^^ (s => toLocalDateTime(s.toLong))
      val end: Parser[LocalDateTime] = "\\d+".r ^^ (s => toLocalDateTime(s.toLong))
      val status: Parser[Status] = "OK|KO".r ^^ {
        case "OK" => OK
        case "KO" => KO
      }
      val error: Parser[String] = "\\D.*".r

      request ~> name ~ userId ~ requestName ~ start ~ end ~ status ~ (error ?) ^^ {
        case n ~ u ~ r ~ s ~ e ~ st ~ err => RequestLog(n, u, r, s, e, st, err)
      }
    }

    def finalLog: Parser[FinalLog] = {
      runLog | userLog | requestLog ^^ (v => v)
    }

    def parseFinalLog(log: String): Log =
      parse(finalLog, log) match {
        case Success(matched, _) => matched
        case Failure(msg, _) => ErrorLog(log, msg)
        case Error(msg, _) => ErrorLog(log, msg)
      }
  }

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

    def errors: Parser[Seq[com.bizagi.gatling.gatling.log.Log.Error]] = {
      val header: Parser[Unit] =
        "-{4} Errors -+".r ^^ (_ => ())

      val start: Parser[Unit] = ">".r ^^ (_ => ())
      val error: Parser[String] = ".+ {2,}".r ^^ (_.trim)
      val percentage: Parser[Double] = "\\(".r ~> "\\d+\\.?\\d+".r <~ "%\\)".r ^^ (_.toDouble)
      val errorCont: Parser[String] = "[^>=].+\n".r ^^ (_.trim)

      val row = start ~> error ~ int ~ percentage ~ (errorCont ?) ^^ {
        case n1 ~ i ~ p ~ e => com.bizagi.gatling.gatling.log.Log.Error(s"$n1${e.getOrElse("")}".trim, i, p)
      }

      header ~> (row +)
    }

    def partialLog: Parser[PartialLog] =
      boundary ~> time ~ testSimulation ~ requests ~ (errors ?) <~ boundary ^^ {
        case t ~ ts ~ r ~ e => PartialLog(t, ts, r, e.getOrElse(Seq.empty))
      }
  }

  def logParser: Parser[Iterable[Log]] = {
    FileLogParser.fileLog | PartialParser.partialLog.map(p => Iterable(p))
  }

  def parseLog(log: String): Iterable[Log] =
    parse(logParser, log) match {
      case Success(matched, _) => matched
      case Failure(msg, _) => Iterable(ErrorLog(log, msg))
      case Error(msg, _) => Iterable(ErrorLog(log, msg))
    }
}
