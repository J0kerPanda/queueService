package db

import java.sql.{Date, Timestamp}

import db.data.AppointmentStatus
import doobie.postgres.implicits.pgEnumStringOpt
import doobie.util.meta.Meta
import org.joda.time.{DateTime, LocalDate, LocalTime, Period}
import org.postgresql.util.{PGInterval, PGTime}

object DatabaseFormats {

  trait IdEntity[K, V <: (Product with Serializable)] {
    def id: K
    def data: V
  }

  //todo handling psql codes!
  implicit val DateTimeMeta: Meta[DateTime] = Meta[Timestamp].xmap(
    ts => new DateTime(ts.getTime),
    dt => new java.sql.Timestamp(dt.getMillis)
  )

  implicit val AppointmentStatusMeta: Meta[AppointmentStatus] = pgEnumStringOpt(
    "appointment_status",
    name => AppointmentStatus.lowerCaseNamesToValuesMap.get(name.toLowerCase),
    enum => enum.dbName
  )

  implicit val PeriodMeta: Meta[Period] = Meta.other[PGInterval]("interval").xmap(
    pgI => new Period(pgI.getYears, pgI.getMonths, 0, pgI.getDays, pgI.getHours, pgI.getMinutes, pgI.getSeconds.toInt, 0),
    period => {
      val p = period.normalizedStandard()
      new PGInterval(p.getYears, p.getMonths, p.getDays + p.getWeeks * 7, p.getHours, p.getMinutes, p.getSeconds)
    }
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
