package db

import java.sql.{Date, Timestamp}

import db.data.{AppointmentStatus, DayOfWeek}
import doobie.postgres.implicits.pgEnumStringOpt
import doobie.util.meta.Meta
import org.joda.time.{DateTime, LocalDate, LocalTime, Period}
import org.postgresql.util.{PGInterval, PGTime}

object DatabaseFormats {

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
    "appointment_status",
    name => DayOfWeek.lowerCaseNamesToValuesMap.get(name.toLowerCase),
    enum => enum.dbName
  )

  implicit val PeriodDaysMeta: Meta[Period] = Meta.other[PGInterval]("interval").xmap(
    pgI => Period.days(pgI.getDays),
    period => {
      val i = new PGInterval()
      i.setDays(period.toStandardDays.getDays)
      i
    }
  )

  implicit val LocalDateMeta: Meta[LocalDate] = Meta[Date].xmap(
    pgD => new LocalDate(pgD.getTime),
    date => new Date(date.toDate.getTime)
  )

  implicit val LocalTimeMeta: Meta[LocalTime] = Meta.other[PGTime]("time").xmap(
    pgT => LocalTime.fromMillisOfDay(pgT.getTime),
    time => new PGTime(time.millisOfDay().get())
  )
}
