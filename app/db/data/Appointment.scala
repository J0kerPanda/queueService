package db.data

import cats.data.NonEmptyList
import db.DatabaseFormats._
import db.data.Appointment.AppointmentId
import db.data.AppointmentStatus.Pending
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.DateTime

object Appointment {

  type AppointmentId = Long

  def insert(a: AppointmentData): ConnectionIO[AppointmentId] = {
    sql"""INSERT INTO "Appointment" (hostid, visitorId, date, status) VALUES (${a.hostId}, ${a.visitorId}, ${a.date}, ${a.status})"""
      .update()
      .withUniqueGeneratedKeys[AppointmentId]("id")
  }

  def selectById(id: AppointmentId): ConnectionIO[Option[Appointment]] = {
    sql"""SELECT "id", hostid, visitorId, date, status FROM "Appointment" WHERE id = $id"""
      .query[Appointment]
      .option
  }

  def selectByIds(ids: NonEmptyList[AppointmentId]): ConnectionIO[List[Appointment]] = {
    (fr"""SELECT "id", hostid, visitorid, date, status FROM "Appointment" WHERE""" ++ Fragments.in(fr"id", ids))
      .query[Appointment]
      .to[List]
  }
}

case class AppointmentData(hostId: UserId,
                           visitorId: UserId,
                           date: DateTime,
                           status: AppointmentStatus = Pending)

case class Appointment(id: AppointmentId, data: AppointmentData) extends IdEntity[AppointmentId, AppointmentData]

