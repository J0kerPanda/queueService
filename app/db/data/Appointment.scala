package db.data

import cats.data.NonEmptyList
import db.DatabaseFormats._
import db.data.Appointment.AppointmentId
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime}

object Appointment {

  type AppointmentId = Long

  private val insertSql = sql"""INSERT INTO "Appointment" (scheduleid, visitorid, start, "end", visited) VALUES """

  private val selectSql = sql"""SELECT "id", scheduleid, visitorId, start, "end", visited FROM "Appointment" """

  private val deleteSql = sql"""DELETE FROM "Appointment" """

  def insert(a: AppointmentData): ConnectionIO[AppointmentId] = {
    (insertSql ++ fr"(${a.scheduleId}, ${a.visitorId}, ${a.start}, ${a.end}, ${a.visited})")
      .update()
      .withUniqueGeneratedKeys[AppointmentId]("id")
  }

  def delete(id: AppointmentId): ConnectionIO[Int] = {
    (deleteSql ++ fr"WHERE id = $id")
      .update()
      .run
  }

  def selectById(id: AppointmentId): ConnectionIO[Option[Appointment]] = {
    (selectSql ++ fr"WHERE id = $id")
      .query[Appointment]
      .option
  }

  def selectByVisitorId(id: UserId, from: LocalDate): ConnectionIO[List[GenericHostAppointment]] = {
    sql"""SELECT A.id, hostid, U.firstname, U.surname, U.patronymic, date, start, "end", visited FROM "Appointment" AS A JOIN "Schedule" AS S ON A.scheduleid = S.id JOIN "User" AS U ON U.id = S.hostid WHERE visitorid = $id AND date >= $from"""
      .query[GenericHostAppointment]
      .to[List]
  }

  def selectByScheduleId(scheduleId: ScheduleId): ConnectionIO[List[GenericVisitorAppointment]] = {
    sql"""SELECT A.id, visitorid, V.firstname, V.surname, V.patronymic, start, "end", visited FROM "Appointment" AS A JOIN "User" AS V ON V.id = A.visitorid WHERE scheduleid = $scheduleId"""
      .query[GenericVisitorAppointment]
      .to[List]
  }

  def deleteOutOfTimeAppointments(scheduleId: ScheduleId, appointmentIntervals: NonEmptyList[AppointmentInterval]): ConnectionIO[Int] = {
    val condition = appointmentIntervals
      .map(i => fr"""start < ${i.start} OR "end" > ${i.end}""")
      .foldSmash(fr"", fr" OR ", fr"")

    (deleteSql ++ Fragments.whereAnd(fr"scheduleId = $scheduleId", condition))
      .update
      .run
  }
}

case class AppointmentData(scheduleId: ScheduleId,
                           visitorId: UserId,
                           start: LocalTime,
                           end: LocalTime,
                           visited: Boolean = false)

case class Appointment(id: AppointmentId, data: AppointmentData) extends IdEntity[AppointmentId, AppointmentData]

case class GenericVisitorAppointment(id: AppointmentId,
                                     visitorId: UserId,
                                     visitorFirstName: String,
                                     visitorSurname: String,
                                     visitorPatronymic: String,
                                     start: LocalTime,
                                     end: LocalTime,
                                     visited: Boolean)

case class GenericHostAppointment(id: AppointmentId,
                                  hostId: UserId,
                                  hostFirstName: String,
                                  hostSurname: String,
                                  hostPatronymic: String,
                                  date: LocalDate,
                                  start: LocalTime,
                                  end: LocalTime,
                                  visited: Boolean)


