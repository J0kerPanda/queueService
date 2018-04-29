package db

import java.sql.Timestamp

import doobie.util.meta.Meta
import org.joda.time.DateTime

object DatabaseUtils {

  //todo handling psql codes!

  implicit val DateTimeMeta: Meta[DateTime] = Meta[Timestamp].xmap(
    ts => new DateTime(ts.getTime),
    dt => new java.sql.Timestamp(dt.getMillis)
  )
}
