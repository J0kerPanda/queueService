package db.data

import db.DatabaseFormats._
import db.data.HostMeta.UserId
import db.data.Schedule.ScheduleId
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime}

object Schedule {

  type ScheduleId = Long

  def insertDefault(s: DefaultScheduleData): ConnectionIO[Option[ScheduleId]] = {
    sql"""INSERT INTO "DefaultSchedule" (hostid, day, start, "end") VALUES (${s.hostId}, ${s.day}, ${s.start}, ${s.end})"""
      .update
      .withUniqueGeneratedKeys("id")
  }

  def selectDefaultById(id: ScheduleId): ConnectionIO[Option[DefaultSchedule]] = {
    sql"""SELECT id, hostid, day, start, "end" FROM "DefaultSchedule" WHERE id = $id"""
      .query[DefaultSchedule]
      .option
  }

  def selectDefaultByDay(day: DayOfWeek): ConnectionIO[Option[DefaultSchedule]] = {
    sql"""SELECT id, hostid, day, start, "end" FROM "DefaultSchedule" WHERE day = $day"""
      .query[DefaultSchedule]
      .option
  }

  def insertCustom(s: CustomScheduleData): ConnectionIO[Option[ScheduleId]] = {
    sql"""INSERT INTO "CustomSchedule" (hostid, date, start, "end") VALUES (${s.hostId}, ${s.date}, ${s.start}, ${s.end})"""
      .update
      .withUniqueGeneratedKeys("id")
  }

  def selectCustomById(id: ScheduleId): ConnectionIO[Option[CustomSchedule]] = {
    sql"""SELECT id, hostid, date, start, "end" FROM "CustomSchedule" WHERE id = $id"""
      .query[CustomSchedule]
      .option
  }

  def selectCustomByDate(date: LocalDate): ConnectionIO[Option[CustomSchedule]] = {
    sql"""SELECT id, hostid, date, start, "end" FROM "CustomSchedule" WHERE date = $date"""
      .query[CustomSchedule]
      .option
  }

  def selectSchedulesOnDate(date: LocalDate): ConnectionIO[(Option[DefaultSchedule], Option[CustomSchedule])] = {
    for {
      d <- selectDefaultByDay(DayOfWeek.fromDate(date))
      c <- selectCustomByDate(date)
    } yield (d, c)
  }
}

case class DefaultScheduleData(hostId: UserId, day: DayOfWeek, start: LocalTime, end: LocalTime)

case class DefaultSchedule(id: ScheduleId, data: DefaultScheduleData) extends IdEntity[ScheduleId, DefaultScheduleData]

case class CustomScheduleData(hostId: UserId, date: LocalDate, start: LocalTime, end: LocalTime)

case class CustomSchedule(id: ScheduleId, data: CustomScheduleData) extends IdEntity[ScheduleId, CustomScheduleData]
