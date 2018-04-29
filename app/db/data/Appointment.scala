package db.data

import java.util.Date

import cats.data.NonEmptyList
import doobie.implicits._
import doobie._
import db.data.AppointmentStatus._
import db.data.AppointmentStatus.AppointmentStatusMeta
import doobie.free.connection.ConnectionIO

object Appointment {

  def insert(a: Appointment): ConnectionIO[Int] = {
    sql"""INSERT INTO "Appointment" (hostid, visitorId, date, status) VALUES (${a.hostId}, ${a.visitorId}, ${a.date}, ${a.status})"""
      .update()
      .run
  }

  def selectById(id: Long): ConnectionIO[Option[Appointment]] = {
    sql"""SELECT "id", hostid, visitorId, date, status FROM "Appointment" WHERE id = $id"""
      .query[Appointment]
      .option
  }

  def selectByIds(ids: NonEmptyList[Long]): ConnectionIO[List[Appointment]] = {
    (fr"""SELECT "id", hostid, visitorid, date, status FROM "Appointment" WHERE""" ++ Fragments.in(fr"id", ids))
      .stripMargin
      .query[Appointment]
      .to[List]
  }

  def forInsertion(hostId: Int, visitorId: Int, date: Date, status: AppointmentStatus = Pending): Appointment = {
    Appointment(-1, hostId, visitorId, date, status)
  }
}

case class Appointment(id: Long,
                       hostId: Int,
                       visitorId: Int,
                       date: Date,
                       status: AppointmentStatus = Pending)
