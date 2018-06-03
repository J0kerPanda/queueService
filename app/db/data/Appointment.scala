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

  private val selectSql = sql"""SELECT "id", scheduleid, visitorId, start, "end" FROM "Appointment" """

  def createBySchedule(visitorId: UserId,
                       scheduleId: ScheduleId,
                       start: LocalTime): ConnectionIO[Option[AppointmentId]] = {

   Schedule
     .select(scheduleId)
     .flatMap {
      case Some(d) => Appointment.insert(AppointmentData(
        scheduleId,
        visitorId,
        start,
        start.plus(d.data.appointmentDuration)
      )).map(Some(_))

      case None => Free.pure(None)
    }
  }

  def insert(a: AppointmentData): ConnectionIO[AppointmentId] = {
    sql"""INSERT INTO "Appointment" (scheduleid, visitorId, start, "end") VALUES (${a.scheduleId}, ${a.visitorId}, ${a.start}, ${a.end})"""
      .update()
      .withUniqueGeneratedKeys[AppointmentId]("id")
  }

  def selectById(id: AppointmentId): ConnectionIO[Option[Appointment]] = {
    (selectSql ++ fr"WHERE id = $id")
      .query[Appointment]
      .option
  }

  def selectByScheduleId(scheduleId: ScheduleId): ConnectionIO[List[GenericAppointment]] = {
    sql"""SELECT visitorid, V.firstname, V.surname, V.patronymic, start, "end" FROM "Appointment" JOIN "User" AS V ON V.id = "Appointment".visitorid WHERE scheduleid = $scheduleId"""
      .query[GenericAppointment]
      .to[List]
  }
}

case class AppointmentData(scheduleId: ScheduleId,
                           visitorId: UserId,
                           start: LocalTime,
                           end: LocalTime)

case class Appointment(id: AppointmentId, data: AppointmentData) extends IdEntity[AppointmentId, AppointmentData]

case class GenericAppointment(visitorId: UserId,
                              visitorFirstName: String,
                              visitorSurname: String,
                              visitorPatronymic: String,
                              start: LocalTime,
                              end: LocalTime)

