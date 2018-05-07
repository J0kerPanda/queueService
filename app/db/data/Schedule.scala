package db.data

import db.DatabaseFormats._
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie.free.connection.ConnectionIO
import doobie._
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime, Period}

object Schedule {

  type ScheduleId = Long

  private val selectDefaultSql = sql"""SELECT id, hostid, day, start, "end", appointmentduration, place FROM "DefaultSchedule""""

  private val selectCustomSql = sql"""SELECT id, hostid, date, start, "end", appointmentduration, place FROM "CustomSchedule""""

  def insertDefault(s: DefaultScheduleData): ConnectionIO[Option[ScheduleId]] = {
    sql"""INSERT INTO "DefaultSchedule" (hostid, day, start, "end", place) VALUES (${s.hostId}, ${s.day}, ${s.start}, ${s.end}, ${s.place})"""
      .update
      .withUniqueGeneratedKeys("id")
  }

  def selectAllDefault(hostId: UserId): ConnectionIO[List[DefaultSchedule]] = {
    (selectDefaultSql ++ fr"WHERE hostid = $hostId")
      .query[DefaultSchedule]
      .to[List]
  }

  def selectDefaultByDay(day: DayOfWeek): ConnectionIO[List[DefaultSchedule]] = {
    (selectDefaultSql ++ fr"WHERE day = $day")
      .query[DefaultSchedule]
      .to[List]
  }

  def insertCustom(s: CustomScheduleData): ConnectionIO[Option[ScheduleId]] = {
    sql"""INSERT INTO "CustomSchedule" (hostid, date, start, "end", place) VALUES (${s.hostId}, ${s.date}, ${s.start}, ${s.end}, ${s.place})"""
      .update
      .withUniqueGeneratedKeys("id")
  }

  def selectCustomById(id: ScheduleId): ConnectionIO[Option[CustomSchedule]] = {
    (selectCustomSql ++ fr"WHERE id = $id")
      .query[CustomSchedule]
      .option
  }

  def selectCustomInPeriod(hostId: UserId, from: LocalDate, to: LocalDate): ConnectionIO[List[CustomSchedule]] = {
    (selectCustomSql ++ Fragments.whereAnd(fr"hostId = $hostId", fr"date >= $from", fr"date < $to"))
      .query[CustomSchedule]
      .to[List]
  }

  def selectCustomByDate(date: LocalDate): ConnectionIO[List[CustomSchedule]] = {
    (selectCustomSql ++ fr"date = $date")
      .query[CustomSchedule]
      .to[List]
  }

  def selectSchedules(hostId: UserId, from: LocalDate, to: LocalDate): ConnectionIO[(List[DefaultScheduleData], List[CustomScheduleData])] = {
    for {
      d <- selectAllDefault(hostId)
      c <- selectCustomInPeriod(hostId, from, to)
    } yield (d.map(_.data), c.map(_.data))
  }

  def selectSchedulesOnDate(date: LocalDate): ConnectionIO[(List[DefaultScheduleData], List[CustomScheduleData])] = {
    for {
      d <- selectDefaultByDay(DayOfWeek.fromDate(date))
      c <- selectCustomByDate(date)
    } yield (d.map(_.data), c.map(_.data))
  }
}


//todo appointment duration

case class DefaultScheduleData(hostId: UserId,
                               day: DayOfWeek,
                               start: LocalTime,
                               end: LocalTime,
                               appointmentDuration: Period,
                               place: String)

case class DefaultSchedule(id: ScheduleId, data: DefaultScheduleData) extends IdEntity[ScheduleId, DefaultScheduleData]

object CustomScheduleData {

  def fromDefault(data: DefaultScheduleData, date: LocalDate): CustomScheduleData = {
    CustomScheduleData(
      hostId = data.hostId,
      date = date,
      start = data.start,
      end = data.end,
      appointmentDuration = data.appointmentDuration,
      place = data.place
    )
  }
}

case class CustomScheduleData(hostId: UserId,
                              date: LocalDate,
                              start: LocalTime,
                              end: LocalTime,
                              appointmentDuration: Period,
                              place: String)

case class CustomSchedule(id: ScheduleId, data: CustomScheduleData) extends IdEntity[ScheduleId, CustomScheduleData]
