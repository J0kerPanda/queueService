package db

import java.sql.Timestamp

import doobie.util.meta.Meta
import org.joda.time.format.{DateTimeFormat, ISOPeriodFormat, PeriodFormat, PeriodFormatterBuilder}
import org.joda.time.{DateTime, Period}
import org.postgresql.util.PGobject

object DatabaseFormats {

  //todo handling psql codes!

  implicit val DateTimeMeta: Meta[DateTime] = Meta[Timestamp].xmap(
    ts => new DateTime(ts.getTime),
    dt => new java.sql.Timestamp(dt.getMillis)
  )

  implicit val PeriodDaysMeta: Meta[Period] = Meta.other[PGobject]("interval").xmap(
    obj => Period.parse(obj.getValue),
    period => {
      val o = new PGobject()
      o.setType("interval")
      o.setValue(period.toStandardDays.toPeriod.toString)
      o
    }
  )
}
