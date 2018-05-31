package db.data

import cats.data.NonEmptyList
import db.DatabaseFormats._
import db.data.RepeatedSchedule.RepeatedScheduleId
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime, Period}

object Schedule {

  type ScheduleId = Int

  private val selectSql = sql"""SELECT id, hostid, repeatid, date, start, "end", appointmentduration, place FROM "Schedule" """

  private val insertSql = sql"""INSERT INTO "Schedule" (hostid, repeatid, date, start, "end", appointmentduration, place) VALUES """

  private val updateSql = sql"""UPDATE "Schedule" AS S """

  private val deleteSql = sql"""DELETE FROM "Schedule" """

  def insert(s: ScheduleData): ConnectionIO[Option[ScheduleId]] = {
    (insertSql ++ fr"(${s.hostId}, ${s.repeatId}, ${s.date}, ${s.start}, ${s.end}, ${s.appointmentDuration}, ${s.place})")
      .update
      .withUniqueGeneratedKeys("id")
  }

  def insertBatch(ss: NonEmptyList[ScheduleData]): ConnectionIO[List[ScheduleId]] = {
    val values = ss
      .map(s =>
        fr"(${s.hostId}, ${s.repeatId}, ${s.date}, ${s.start}, ${s.end}, ${s.appointmentDuration}, ${s.place})"
      )

    (insertSql ++ values.foldSmash(fr"", fr", ", fr""))
      .update
      .withGeneratedKeysWithChunkSize[ScheduleId]("id")(values.size)
      .compile.fold(List[ScheduleId]())((acc, id) => id :: acc)
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
                        start: LocalTime,
                        end: LocalTime,
                        appointmentDuration: Period,
                        place: String)
