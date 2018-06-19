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

  private val selectSql = sql"""SELECT id, hostid, repeatid, date, appointmentintervals, appointmentduration, place, isBlocked FROM "Schedule" """

  private val insertSql = sql"""INSERT INTO "Schedule" (hostid, repeatid, date, appointmentintervals, appointmentduration, place, isblocked) VALUES """

  private val updateSql = sql"""UPDATE "Schedule" AS S """

  private val deleteSql = sql"""DELETE FROM "Schedule" """

  def insert(s: ScheduleData): ConnectionIO[ScheduleId] = {
    (insertSql ++ fr"(${s.hostId}, ${s.repeatId}, ${s.date}, ${s.appointmentIntervals}::timerange[], ${s.appointmentDuration}, ${s.place}, ${s.isBlocked})")
      .update
      .withUniqueGeneratedKeys("id")
  }

  def insertBatch(ss: NonEmptyList[ScheduleData]): ConnectionIO[List[ScheduleId]] = {
    val values = ss
      .map(s =>
        fr"(${s.hostId}, ${s.repeatId}, ${s.date}, ${s.appointmentIntervals}::timerange[], ${s.appointmentDuration}, ${s.place}. ${s.isBlocked})"
      )

    (insertSql ++ values.foldSmash(fr"", fr", ", fr""))
      .update
      .withGeneratedKeysWithChunkSize[ScheduleId]("id")(values.size)
      .compile.fold(List[ScheduleId]())((acc, id) => id :: acc)
  }

  def update(s: Schedule): ConnectionIO[ScheduleId] = {
    val d = s.data
    (updateSql
      ++ fr"SET hostId = ${d.hostId}"
      ++ fr"repeatId = ${d.repeatId} "
      ++ fr"date = ${d.date} "
      ++ fr"appointmentIntervals = ${d.appointmentIntervals} "
      ++ fr"appointmentDuration = ${d.appointmentDuration} "
      ++ fr"place = ${d.place} "
      ++ fr"isBlocked = ${d.isBlocked} "
      ++ fr"WHERE id = ${s.id}"
    )
      .update
      .run
  }


  def select(id: ScheduleId): ConnectionIO[Option[Schedule]] = {
    (selectSql ++ fr"WHERE id = $id")
      .query[Schedule]
      .option
  }

  def selectInPeriod(hostId: UserId, from: LocalDate, to: LocalDate): ConnectionIO[List[Schedule]] = {
    (selectSql ++ Fragments.whereAnd(fr"hostId = $hostId", fr"date >= $from", fr"date < $to", fr"isblocked = FALSE"))
      .query[Schedule]
      .to[List]
  }

  def blockRepeatedByDate(date: LocalDate): ConnectionIO[Int] = {
    (updateSql
      ++ fr"SET isblocked = TRUE "
      ++ Fragments.whereAnd(fr"date = $date", fr"repeatId IS NOT NULL"))
      .update
      .run
  }
}
case class Schedule(id: ScheduleId, data: ScheduleData) extends IdEntity[ScheduleId, ScheduleData]

case class ScheduleData(hostId: UserId,
                        repeatId: Option[RepeatedScheduleId],
                        date: LocalDate,
                        appointmentIntervals: List[AppointmentInterval],
                        appointmentDuration: Period,
                        place: String,
                        isBlocked: Boolean = false)
