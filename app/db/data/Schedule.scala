package db.data

import db.DatabaseFormats._
import db.data.HostMeta.UserId
import db.data.Schedule.ScheduleId
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime}

object Schedule {

  type ScheduleId = Long

  def selectDefaultById(id: ScheduleId): ConnectionIO[Option[DefaultSchedule]] = {
    sql"""SELECT id, hostid, day, start, stop FROM "DefaultSchedule" WHERE id = $id"""
      .query[DefaultSchedule]
      .option
  }

  def selectDefaultByDay(day: DayOfWeek): ConnectionIO[Option[DefaultSchedule]] = {
    sql"""SELECT id, hostid, day, start, stop FROM "DefaultSchedule" WHERE day = $day"""
      .query[DefaultSchedule]
      .option
  }

  def selectCustomById(id: ScheduleId): ConnectionIO[Option[CustomSchedule]] = {
    sql"""SELECT id, hostid, date, start, stop FROM "CustomSchedule" WHERE id = $id"""
      .query[CustomSchedule]
      .option
  }

  def selectCustomByDate(date: LocalDate): ConnectionIO[Option[CustomSchedule]] = {
    sql"""SELECT id, hostid, date, start, stop FROM "CustomSchedule" WHERE date = $date"""
      .query[CustomSchedule]
      .option
  }

  def selectOnDate(date: LocalDate): ConnectionIO[(Option[DefaultSchedule], Option[CustomSchedule])] = {
    for {
      d <- selectDefaultByDay(DayOfWeek.fromDate(date))
      c <- selectCustomByDate(date)
    } yield (d, c)
  }
}

case class DefaultSchedule(id: ScheduleId, hostId: UserId, day: DayOfWeek, start: LocalTime, stop: LocalTime)

case class CustomSchedule(id: ScheduleId, hostId: UserId, date: LocalDate, start: LocalTime, stop: LocalTime)
