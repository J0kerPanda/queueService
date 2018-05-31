package db.data

import db.DatabaseFormats._
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime, Period}

object RepeatedSchedule {

  type RepeatScheduleId = Int

  private val selectSql = sql"""SELECT id, hostid, repeatdate, repeatperiod, start, "end", appointmentduration, place FROM "RepeatedSchedule""""

  def insert(repeatSchedule: RepeatedScheduleData): ConnectionIO[RepeatScheduleId] = ???

  def selectBeforeDate(date: LocalDate): ConnectionIO[List[RepeatedSchedule]] = ???

  def generateSchedules(): ConnectionIO[List[ScheduleId]] = ???
}

case class RepeatedScheduleData(hostId: UserId,
                                repeatDate: LocalDate,
                                repeatPeriod: Period,
                                start: LocalTime,
                                end: LocalTime,
                                appointmentDuration: Period,
                                place: String)

case class RepeatedSchedule(id: ScheduleId, data: RepeatedScheduleData) extends IdEntity[ScheduleId, RepeatedScheduleData]
