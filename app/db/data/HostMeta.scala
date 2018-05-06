package db.data

import cats.data.NonEmptyList
import db.DatabaseFormats._
import db.data.User.UserId
import doobie._
import doobie.implicits._
import org.joda.time.Period

object HostMeta {

  def insert(m: HostMeta): ConnectionIO[UserId] = {
    sql"""INSERT INTO "HostMeta" (id, appointmentinterval) VALUES (${m.id}, ${m.appointmentInterval})"""
      .update()
      .withUniqueGeneratedKeys[UserId]("id")
  }

  def selectById(id: UserId): ConnectionIO[Option[HostMeta]] = {
    sql"""SELECT id, appointmentinterval FROM "HostMeta" WHERE id = $id"""
      .query[HostMeta]
      .option
  }

  def selectByIds(ids: NonEmptyList[UserId]): ConnectionIO[List[HostMeta]] = {
    (fr"""SELECT id, appointmentinterval FROM "HostMeta" WHERE""" ++ Fragments.in(fr"id", ids))
      .query[HostMeta]
      .to[List]
  }
}

case class HostMeta(id: UserId, appointmentInterval: Period)
