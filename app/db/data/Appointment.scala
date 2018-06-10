package db.data

import cats.free.Free
import db.DatabaseFormats._
import db.data.Appointment.AppointmentId
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import org.joda.time.LocalTime

object Appointment {

  type AppointmentId = Long

  private val selectSql = sql"""SELECT "id", scheduleid, visitorId, hostId, start, "end" FROM "Appointment" """

  def createBySchedule(visitorId: UserId,
                       scheduleId: ScheduleId,
                       hostId: UserId,
                       start: LocalTime): ConnectionIO[Option[AppointmentId]] = {

   Schedule
     .select(scheduleId)
     .flatMap {
      case Some(d) => Appointment.insert(AppointmentData(
        scheduleId,
        visitorId,
        hostId,
        start,
        start.plus(d.data.appointmentDuration)
      )).map(Some(_))

      case None => Free.pure(None)
    }
  }

  def insert(a: AppointmentData): ConnectionIO[AppointmentId] = {
    sql"""INSERT INTO "Appointment" (scheduleid, visitorid, hostid, start, "end") VALUES (${a.scheduleId}, ${a.visitorId}, ${a.hostId}, ${a.start}, ${a.end})"""
      .update()
      .withUniqueGeneratedKeys[AppointmentId]("id")
  }

  def checkAppointmentUser(id: AppointmentId, userId: UserId): ConnectionIO[Option[Boolean]] = {
    sql"""SELECT (visitorid = $userId OR hostid = $userId) FROM "Appointment""""
      .query[Boolean]
      .option
  }

  def delete(id: AppointmentId): ConnectionIO[Int] = {
    sql"""DELETE FROM "Appointment" WHERE id = $id"""
      .update()
      .run
  }

  def selectById(id: AppointmentId): ConnectionIO[Option[Appointment]] = {
    (selectSql ++ fr"WHERE id = $id")
      .query[Appointment]
      .option
  }

  def selectByVisitorId(id: UserId): ConnectionIO[List[GenericHostAppointment]] = {
    sql"""SELECT A.id, hostid, U.firstname, U.surname, U.patronymic, start, "end" FROM "Appointment" AS A JOIN "User" AS U ON U.id = A.hostid WHERE visitorid = $id"""
      .query[GenericHostAppointment]
      .to[List]
  }

  def selectByScheduleId(scheduleId: ScheduleId): ConnectionIO[List[GenericVisitorAppointment]] = {
    sql"""SELECT A.id, visitorid, V.firstname, V.surname, V.patronymic, start, "end" FROM "Appointment" AS A JOIN "User" AS V ON V.id = A.visitorid WHERE scheduleid = $scheduleId"""
      .query[GenericVisitorAppointment]
      .to[List]
  }
}

case class AppointmentData(scheduleId: ScheduleId,
                           visitorId: UserId,
                           hostId: UserId,
                           start: LocalTime,
                           end: LocalTime)

case class Appointment(id: AppointmentId, data: AppointmentData) extends IdEntity[AppointmentId, AppointmentData]

case class GenericVisitorAppointment(id: AppointmentId,
                                     visitorId: UserId,
                                     visitorFirstName: String,
                                     visitorSurname: String,
                                     visitorPatronymic: String,
                                     start: LocalTime,
                                     end: LocalTime)

case class GenericHostAppointment(id: AppointmentId,
                                  hostId: UserId,
                                  hostFirstName: String,
                                  hostSurname: String,
                                  hostPatronymic: String,
                                  start: LocalTime,
                                  end: LocalTime)


