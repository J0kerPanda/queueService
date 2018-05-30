package db.data

import db.DatabaseFormats._
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime, Period}

object Schedule {

  type ScheduleId = Int

  private val selectSql = sql"""SELECT id, hostid, date, start, "end", appointmentduration, place FROM "Schedule""""

  def insert(s: ScheduleData): ConnectionIO[Option[ScheduleId]] = {
    sql"""INSERT INTO "Schedule" (hostid, date, start, "end", appointmentduration, place) VALUES (${s.hostId}, ${s.date}, ${s.start}, ${s.end}, ${s.appointmentDuration}, ${s.place})"""
      .update
      .withUniqueGeneratedKeys("id")
  }

  def select(id: ScheduleId): ConnectionIO[Option[Schedule]] = {
    (selectSql ++ fr"WHERE id = $id")
      .query[Schedule]
      .option
  }

  def selectInPeriod(hostId: UserId, from: LocalDate, to: LocalDate): ConnectionIO[List[Schedule]] = {
    (selectSql ++ Fragments.whereAnd(fr"hostId = $hostId", fr"date >= $from", fr"date < $to"))
      .query[Schedule]
      .to[List]
  }

  def selectByDate(date: LocalDate): ConnectionIO[List[Schedule]] = {
    (selectSql ++ fr"date = $date")
      .query[Schedule]
      .to[List]
  }

  def selectSchedules(hostId: UserId, from: LocalDate, to: LocalDate): ConnectionIO[List[Schedule]] = ???
}
case class Schedule(id: ScheduleId, data: ScheduleData) extends IdEntity[ScheduleId, ScheduleData]

case class ScheduleData(hostId: UserId,
                        date: LocalDate,
                        start: LocalTime,
                        end: LocalTime,
                        appointmentDuration: Period,
                        place: String)
