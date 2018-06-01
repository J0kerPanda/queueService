package db.data

import cats.data.NonEmptyList
import cats.free.Free
import db.DatabaseFormats._
import db.data.Appointment.AppointmentId
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import org.joda.time.{LocalDate, LocalTime}

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
        d.data.date,
        start,
        start.plus(d.data.appointmentDuration)
      )).map(Some(_))

      case None => Free.pure(None)
    }
  }

  def insert(a: AppointmentData): ConnectionIO[AppointmentId] = {
    sql"""INSERT INTO "Appointment" (scheduleid, visitorId, date, start, "end") VALUES (${a.scheduleId}, ${a.visitorId}, ${a.date}, ${a.start}, ${a.end})"""
      .update()
      .withUniqueGeneratedKeys[AppointmentId]("id")
  }

  def selectById(id: AppointmentId): ConnectionIO[Option[Appointment]] = {
    (selectSql ++ fr"WHERE id = $id")
      .query[Appointment]
      .option
  }

  def selectByIds(ids: NonEmptyList[AppointmentId]): ConnectionIO[List[Appointment]] = {
    (selectSql ++ Fragments.whereAnd(Fragments.in(fr"id", ids)))
      .query[Appointment]
      .to[List]
  }

  def selectScheduleIds(scheduleIds: NonEmptyList[ScheduleId]): ConnectionIO[List[GenericAppointment]] = {
    (sql"""SELECT visitorid, V.firstname, V.surname, V.patronymic, start, "end" FROM "Appointment" JOIN "User" AS V ON V.id = "Appointment".visitorid """ ++ Fragments.whereAnd(Fragments.in(fr"scheduleid", scheduleIds)))
      .query[GenericAppointment]
      .to[List]
  }
}

case class AppointmentData(scheduleId: ScheduleId,
                           visitorId: UserId,
                           date: LocalDate,
                           start: LocalTime,
                           end: LocalTime)

case class Appointment(id: AppointmentId, data: AppointmentData) extends IdEntity[AppointmentId, AppointmentData]

case class GenericAppointment(visitorId: UserId,
                              visitorFirstName: String,
                              visitorSurname: String,
                              visitorPatronymic: String,
                              start: LocalTime,
                              end: LocalTime)

