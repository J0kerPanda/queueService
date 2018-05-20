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

  private val selectAppointmentSql = sql"""SELECT "id", hostid, visitorId, date, start, "end" FROM "Appointment""""

  //todo check date/start/end
  def createBySchedule(hostId: UserId,
                       visitorId: UserId,
                       scheduleId: ScheduleId,
                       isCustom: Boolean,
                       date: LocalDate,
                       start: LocalTime): ConnectionIO[Option[AppointmentId]] = {

    val selectSchedule: ConnectionIO[Option[GeneralScheduleData]] = if (isCustom) {
      Schedule.selectCustomById(scheduleId).map(_.map(_.data))
    } else {
      Schedule.selectDefaultById(scheduleId).map(_.map(_.data))
    }

    selectSchedule.flatMap {
      case Some(d) => Appointment.insert(AppointmentData(
        hostId,
        visitorId,
        date,
        start,
        start.plus(d.appointmentDuration)
      )).map(Some(_))

      case None => Free.pure(None)
    }
  }

  def insert(a: AppointmentData): ConnectionIO[AppointmentId] = {
    sql"""INSERT INTO "Appointment" (hostid, visitorId, date, start, "end") VALUES (${a.hostId}, ${a.visitorId}, ${a.date}, ${a.start}, ${a.end})"""
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

case class AppointmentData(hostId: UserId,
                           visitorId: UserId,
                           date: LocalDate,
                           start: LocalTime,
                           end: LocalTime)

case class Appointment(id: AppointmentId, data: AppointmentData) extends IdEntity[AppointmentId, AppointmentData]

case class GenericAppointment(visitorId: Option[UserId],
                              visitorFullName: Option[String],
                              start: LocalTime,
                              end: LocalTime)

