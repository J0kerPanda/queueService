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

  private val selectDefaultSql = sql"""SELECT id, hostid, firstdate, repeatperiod, start, "end", appointmentduration, place FROM "DefaultSchedule""""

  //change select schedules
  private val selectCustomSql = sql"""SELECT id, hostid, date, start, "end", appointmentduration, place FROM "CustomSchedule""""

  def insertDefault(s: DefaultScheduleData): ConnectionIO[Option[ScheduleId]] = {
    sql"""INSERT INTO "DefaultSchedule" (hostid, firstDate, repeatperiod, start, "end", appointmentduration, place) VALUES (${s.hostId}, ${s.firstDate}, ${s.repeatPeriod}, ${s.start}, ${s.end}, ${s.appointmentDuration}, ${s.place})"""
      .update
      .withUniqueGeneratedKeys("id")
  }

  def selectDefaultById(id: ScheduleId): ConnectionIO[Option[DefaultSchedule]] = {
    (selectDefaultSql ++ fr"WHERE id = $id")
      .query[DefaultSchedule]
      .option
  }

  def selectAllDefault(hostId: UserId): ConnectionIO[List[DefaultSchedule]] = {
    (selectDefaultSql ++ fr"WHERE hostid = $hostId")
      .query[DefaultSchedule]
      .to[List]
  }

  def insertCustom(s: CustomScheduleData): ConnectionIO[Option[ScheduleId]] = {
    sql"""INSERT INTO "CustomSchedule" (hostid, date, start, "end", appointmentduration, place) VALUES (${s.hostId}, ${s.date}, ${s.start}, ${s.end}, ${s.appointmentDuration}, ${s.place})"""
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

  def selectSchedules(hostId: UserId, from: LocalDate, to: LocalDate): ConnectionIO[List[GenericSchedule]] = {
    sql"""SELECT GEN.c_id, GEN.c_date, GEN.c_start, GEN.c_end, GEN.c_appointmentduration, GEN.c_place, GEN.c_iscustom FROM get_schedule($hostId, $from, $to) AS GEN"""
      .query[GenericSchedule]
      .to[List]
  }
}

//todo single query schedule
//todo default schedule interval

trait GeneralScheduleData {
  def start: LocalTime
  def end: LocalTime
  def appointmentDuration: Period
  def place: String
}

case class DefaultScheduleData(hostId: UserId,
                               firstDate: LocalDate,
                               repeatPeriod: Period,
                               start: LocalTime,
                               end: LocalTime,
                               appointmentDuration: Period,
                               place: String) extends GeneralScheduleData

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
                              place: String) extends GeneralScheduleData

case class CustomSchedule(id: ScheduleId, data: CustomScheduleData) extends IdEntity[ScheduleId, CustomScheduleData]

case class GenericSchedule(rootId: ScheduleId,
                           date: LocalDate,
                           start: LocalTime,
                           end: LocalTime,
                           appointmentDuration: Period,
                           place: String,
                           isCustom: Boolean) extends GeneralScheduleData
