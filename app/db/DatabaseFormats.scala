package db

import java.sql.{Date, Timestamp}

import cats.data.NonEmptyList
import db.data.AppointmentInterval
import doobie.util.meta.Meta
import org.joda.time.{DateTime, LocalDate, LocalTime, Period}
import org.postgresql.util.{PGInterval, PGTime, PGobject}

object DatabaseFormats {

  trait IdEntity[K, V] {
    def id: K
    def data: V
  }

  implicit val DateTimeMeta: Meta[DateTime] = Meta[Timestamp].xmap(
    ts => new DateTime(ts.getTime),
    dt => new java.sql.Timestamp(dt.getMillis)
  )

  implicit val PeriodMeta: Meta[Period] = Meta.other[PGInterval]("interval").xmap(
    pgI => new Period(
      pgI.getYears,
      pgI.getMonths,
      0,
      pgI.getDays,
      pgI.getHours,
      pgI.getMinutes,
      pgI.getSeconds.toInt,
      0
    ).normalizedStandard(),
    period => {
      val p = period.normalizedStandard()
      new PGInterval(
        p.getYears,
        p.getMonths,
        p.getDays + p.getWeeks * 7,
        p.getHours,
        p.getMinutes,
        p.getSeconds
      )
    }
  )

  private def parseTimeRange(str: String): AppointmentInterval = {
    val se = str.drop(1).dropRight(1).split(",")
    AppointmentInterval(LocalTime.parse(se(0)), LocalTime.parse(se(1)))
  }

  private def convertToTimeRange(ai: AppointmentInterval): String = {
    s"[${ai.start}, ${ai.end}]"
  }

  implicit val AppointmentIntervalMeta: Meta[AppointmentInterval] = Meta.other[PGobject]("timerange").xmap(
    tr => parseTimeRange(tr.getValue),
    ai => {
      val res = new PGobject()
      res.setType("timerange")
      res.setValue(convertToTimeRange(ai))
      res
    }
  )

  implicit val AppointmentIntervalArrayMeta: Meta[NonEmptyList[AppointmentInterval]] = Meta.StringMeta.xmap(
    str => {
      val parsedList = str // {"[...]","[...]", ...}
        .drop(2).dropRight(2) //drop {" "}
        .split("\",\"")
        .map(parseTimeRange)
        .toList

      NonEmptyList.fromListUnsafe(parsedList)
    },
    aiList => s"""{"${aiList.map(convertToTimeRange).toList.mkString("\",\"")}"}"""
  )

  implicit val LocalTimeMeta: Meta[LocalTime] = Meta.TimeMeta.xmap(
    pgTime => new LocalTime(pgTime.getTime),
    time => new PGTime(time.toDateTimeToday.getMillis)
  )

  implicit val LocalDateMeta: Meta[LocalDate] = Meta[Date].xmap(
    pgD => new LocalDate(pgD.getTime),
    date => new Date(date.toDate.getTime)
  )
}
