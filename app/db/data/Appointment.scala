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

  private val selectAppointmentSql = sql"""SELECT "id", scheduleid, visitorId, start, "end" FROM "Appointment""""

  //todo check date/start/end
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
    (selectAppointmentSql ++ fr"WHERE id = $id")
      .query[Appointment]
      .option
  }

  def selectByIds(ids: NonEmptyList[AppointmentId]): ConnectionIO[List[Appointment]] = {
    (selectAppointmentSql ++ Fragments.whereAnd(Fragments.in(fr"id", ids)))
      .query[Appointment]
      .to[List]
  }

  def selectByDate(hostId: UserId, date: LocalDate): ConnectionIO[List[GenericAppointment]] = {
    sql"""SELECT visitorid, format('%s %s %s', V.surname, V.firstname, V.patronymic)::VARCHAR(255), start, "end" FROM "Appointment" JOIN "User" AS V ON V.id = "Appointment".visitorid"""
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

case class GenericAppointment(visitorId: Option[UserId],
                              visitorFullName: Option[String],
                              start: LocalTime,
                              end: LocalTime)

