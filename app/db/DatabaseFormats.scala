package db

import java.sql.Timestamp

import db.data.AppointmentStatus
import doobie.postgres.implicits.pgEnumStringOpt
import doobie.util.meta.Meta
import org.joda.time.{DateTime, Period}
import org.postgresql.util.PGInterval


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

  implicit val DayOfWeekMeta: Meta[AppointmentStatus] = pgEnumStringOpt(
    "appointment_status",
    name => AppointmentStatus.lowerCaseNamesToValuesMap.get(name.toLowerCase),
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
}
