package db.data

import cats.data.NonEmptyList
import db.DatabaseFormats._
import db.data.RepeatedSchedule.RepeatedScheduleId
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, Period}

object Schedule {

  type ScheduleId = Int

  private val selectSql = sql"""SELECT id, hostid, repeatid, date, appointmentintervals, appointmentduration, place FROM "Schedule" """

  private val insertSql = sql"""INSERT INTO "Schedule" (hostid, repeatid, date, appointmentintervals, appointmentduration, place) VALUES """

  private val updateSql = sql"""UPDATE "Schedule" AS S """

  private val deleteSql = sql"""DELETE FROM "Schedule" """

  def insert(s: ScheduleData): ConnectionIO[ScheduleId] = {
    (insertSql ++ fr"(${s.hostId}, ${s.repeatId}, ${s.date}, ${s.appointmentIntervals}::timerange[], ${s.appointmentDuration}, ${s.place})")
      .update
      .withUniqueGeneratedKeys("id")
  }

  def insertBatch(ss: NonEmptyList[ScheduleData]): ConnectionIO[List[ScheduleId]] = {
    val values = ss
      .map(s =>
        fr"(${s.hostId}, ${s.repeatId}, ${s.date}, ${s.appointmentIntervals}::timerange[], ${s.appointmentDuration}, ${s.place})"
      )

    (insertSql ++ values.foldSmash(fr"", fr", ", fr""))
      .update
      .withGeneratedKeysWithChunkSize[ScheduleId]("id")(values.size)
      .compile.fold(List[ScheduleId]())((acc, id) => id :: acc)
  }

  def update(s: Schedule): ConnectionIO[Int] = {
    val d = s.data
    (updateSql
      ++ fr"SET hostId = ${d.hostId}, "
      ++ fr"repeatId = ${d.repeatId}, "
      ++ fr"date = ${d.date}, "
      ++ fr"appointmentIntervals = ${d.appointmentIntervals}::timerange[], "
      ++ fr"appointmentDuration = ${d.appointmentDuration}, "
      ++ fr"place = ${d.place}, "
      ++ fr"WHERE id = ${s.id}"
    )
      .update
      .run
  }

  def delete(id: ScheduleId): ConnectionIO[Int] = {
    (deleteSql ++ fr"WHERE id = $id")
      .update
      .run
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

  def removeRepeatId(repeatId: ScheduleId): ConnectionIO[Int] = {
    (updateSql ++ fr"SET repeatId = NULL WHERE repeatId = $repeatId")
      .update
      .run
  }

  def selectOccupiedDates(from: LocalDate): ConnectionIO[List[(UserId, LocalDate)]] = {
    sql"""SELECT hostid, date FROM "Schedule" WHERE date >= $from"""
      .query[(UserId, LocalDate)]
      .to[List]
  }
}
case class Schedule(id: ScheduleId, data: ScheduleData) extends IdEntity[ScheduleId, ScheduleData]

case class ScheduleData(hostId: UserId,
                        repeatId: Option[RepeatedScheduleId],
                        date: LocalDate,
                        appointmentIntervals: NonEmptyList[AppointmentInterval],
                        appointmentDuration: Period,
                        place: String)
