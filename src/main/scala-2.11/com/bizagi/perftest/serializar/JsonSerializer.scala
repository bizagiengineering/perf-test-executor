package com.bizagi.perftest.serializar

import java.time.LocalDateTime

import com.bizagi.gatling.gatling.log.Log.{KO, Log, OK, Status}
import net.liftweb.json.Extraction._
import net.liftweb.json.{DefaultFormats, _}

import scala.concurrent.duration._

/**
  * Created by dev-williame on 2/13/17.
  */
object JsonSerializer {

  implicit val formats = DefaultFormats

  def toJson(log: Log) = {
    implicit val formats = Serialization.formats(NoTypeHints) + new DurationSerializer + new LocalDateTimeSerializer + new StatusSerializer
    prettyRender(decompose(log))
  }

  class DurationSerializer extends Serializer[Duration] {
    override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Duration] = ???
    override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case x: Duration =>
        JObject(JField("time", JInt(x._1)) :: JField("unit", JString(x._2.toString)) :: Nil)
    }
  }

  class LocalDateTimeSerializer extends Serializer[LocalDateTime] {
    private val LocalDateTimeClass = classOf[LocalDateTime]

    override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), LocalDateTime] = ???
    override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case x: LocalDateTime =>
        JString(x.toString)
    }
  }

  class StatusSerializer extends Serializer[Status] {
    private val StatusClass = classOf[Status]

    override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Status] = {
      case (TypeInfo(StatusClass, _), json) => json match {
        case JString("OK") => OK
        case JString("KO") => KO
      }
    }
    override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case OK => JString("OK")
      case KO => JString("KO")
    }
  }
}
