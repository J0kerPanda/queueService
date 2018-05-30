package db.data

import db.DatabaseFormats._
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime, Period}

object RepeatSchedule {

  type RepeatScheduleId = Int

  private val selectSql = sql"""SELECT id, hostid, repeatdate, repeatperiod, start, "end", appointmentduration, place FROM "RepeatSchedule""""

  def insert(repeatSchedule: RepeatSchedule): ConnectionIO[RepeatScheduleId] = ???

  def selectBeforeDate(date: LocalDate): ConnectionIO[List[RepeatSchedule]] = ???

  def generateSchedules(): ConnectionIO[List[ScheduleId]] = ???
}

case class RepeatScheduleData(hostId: UserId,
                              firstDate: LocalDate,
                              repeatPeriod: Period,
                              start: LocalTime,
                              end: LocalTime,
                              appointmentDuration: Period,
                              place: String)

case class RepeatSchedule(id: ScheduleId, data: RepeatScheduleData) extends IdEntity[ScheduleId, RepeatScheduleData]
