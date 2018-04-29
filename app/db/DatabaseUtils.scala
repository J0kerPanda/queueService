package db

import doobie.implicits._

object DatabaseUtils {

  val returnLastIntId: doobie.ConnectionIO[Int] = sql"""SELECT lastval()""".query[Int].unique

  val returnLastLongId: doobie.ConnectionIO[Long] = sql"""SELECT lastval()""".query[Long].unique
}
