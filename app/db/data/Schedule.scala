package db.data

import db.data.HostMeta.UserId
import db.data.Schedule.ScheduleId
import doobie.free.connection.ConnectionIO
import org.joda.time.{DateTime, LocalTime}
import db.DatabaseFormats.{DateTimeMeta, DayOfWeekMeta, LocalTimeMeta}

import doobie.implicits._

object Schedule {

  type ScheduleId = Long

  def selectDefaultById(id: ScheduleId): ConnectionIO[Option[DefaultSchedule]] = {
    sql"""SELECT id, hostid, day, start, stop FROM "DefaultSchedule" WHERE id = $id"""
      .query[DefaultSchedule]
      .option
  }

  def selectCustomById(id: ScheduleId): ConnectionIO[Option[CustomSchedule]] = {
    sql"""SELECT id, hostid, date, start, stop FROM "CustomSchedule" WHERE id = $id"""
      .query[CustomSchedule]
      .option
  }
}

case class DefaultSchedule(id: ScheduleId, hostId: UserId, day: DayOfWeek, start: LocalTime, stop: LocalTime)

case class CustomSchedule(id: ScheduleId, hostId: UserId, date: DateTime, start: LocalTime, stop: LocalTime)
