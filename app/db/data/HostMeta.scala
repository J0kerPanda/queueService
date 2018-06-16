package db.data

import cats.data.NonEmptyList
import db.DatabaseFormats._
import db.data.User.UserId
import doobie._
import doobie.implicits._
import org.joda.time.Period

object HostMeta {

  private val selectHostMetaSql = sql"""SELECT id, appointmentperiod FROM "HostMeta""""

  def insert(m: HostMeta): ConnectionIO[UserId] = {
    sql"""INSERT INTO "HostMeta" (id, appointmentperiod) VALUES (${m.id}, ${m.appointmentPeriod})"""
      .update()
      .withUniqueGeneratedKeys[UserId]("id")
  }

  def selectById(id: UserId): ConnectionIO[Option[HostMeta]] = {
    (selectHostMetaSql ++ fr"WHERE id = $id")
      .query[HostMeta]
      .option
  }

  def selectByIds(ids: NonEmptyList[UserId]): ConnectionIO[List[HostMeta]] = {
    (selectHostMetaSql ++ Fragments.whereAnd(Fragments.in(fr"id", ids)))
      .query[HostMeta]
      .to[List]
  }
}

case class HostMeta(id: UserId, appointmentPeriod: Period)
