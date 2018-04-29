package db.data

import java.util.Date

import cats.data.NonEmptyList
import db.data.Appointment.AppointmentId
import doobie.implicits._
import doobie._
import db.data.AppointmentStatus._
import db.data.User.UserId
import doobie.free.connection.ConnectionIO

object Appointment {

  type AppointmentId = Long

  def insert(a: Appointment): ConnectionIO[AppointmentId] = {
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
      .stripMargin
      .query[Appointment]
      .to[List]
  }

  def forInsertion(hostId: UserId, visitorId: UserId, date: Date, status: AppointmentStatus = Pending): Appointment = {
    Appointment(-1, hostId, visitorId, date, status)
  }
}

case class Appointment(id: AppointmentId,
                       hostId: UserId,
                       visitorId: UserId,
                       date: Date,
                       status: AppointmentStatus = Pending)
