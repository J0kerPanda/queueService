package db

import java.sql.{Date, Timestamp}

import db.data.{AppointmentStatus, DayOfWeek}
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

  implicit val DayOfWeekMeta: Meta[DayOfWeek] = pgEnumStringOpt(
    "day_of_week",
    name => DayOfWeek.lowerCaseNamesToValuesMap.get(name.toLowerCase),
    enum => enum.dbName
  )

  implicit val PeriodDaysMeta: Meta[Period] = Meta.other[PGInterval]("interval").xmap(
    pgInterval => Period.days(pgInterval.getDays),
    period => new PGInterval(0, 0, period.getDays, 0, 0, 0)
  )

  implicit val LocalTimeMeta: Meta[LocalTime] = Meta.other[PGTime]("time").xmap(
    pgTime => LocalTime.fromMillisOfDay(pgTime.getTime),
    time => new PGTime(time.toDateTimeToday.getMillis)
  )

  implicit val LocalDateMeta: Meta[LocalDate] = Meta[Date].xmap(
    pgD => new LocalDate(pgD.getTime),
    date => new Date(date.toDate.getTime)
  )
}
