package db

import java.sql.Timestamp

import doobie.util.meta.Meta
import org.joda.time.{DateTime, Period}
import org.postgresql.util.PGInterval


object DatabaseFormats {

  //todo handling psql codes!

  implicit val DateTimeMeta: Meta[DateTime] = Meta[Timestamp].xmap(
    ts => new DateTime(ts.getTime),
    dt => new java.sql.Timestamp(dt.getMillis)
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
