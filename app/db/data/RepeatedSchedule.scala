package db.data

import cats.data.NonEmptyList
import cats.free.Free
import db.DatabaseFormats._
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime, Period}
import io.scalaland.chimney.dsl._

object RepeatedSchedule {

  type RepeatedScheduleId = Int

  private val selectSql = sql"""SELECT id, hostid, repeatdate, repeatperiod, start, "end", appointmentduration, place FROM "RepeatedSchedule" """

  def insert(s: RepeatedScheduleData): ConnectionIO[RepeatedScheduleId] = {
    sql"""INSERT INTO "RepeatedSchedule" (hostid, repeatdate, repeatperiod, start, "end", appointmentduration, place) VALUES (${s.hostId}, ${s.repeatDate}, ${s.repeatPeriod}, ${s.start}, ${s.end}, ${s.appointmentDuration}, ${s.place})"""
      .update
      .withUniqueGeneratedKeys("id")
  }

  def updateBatch(ss: NonEmptyList[RepeatedScheduleData]):  ConnectionIO[List[RepeatedScheduleId]] = {
    val values = ss
      .map(s =>
        fr"${s.hostId}, ${s.repeatDate}, ${s.repeatPeriod}, ${s.start}, ${s.end}, ${s.appointmentDuration}, ${s.place}"
      )


    (sql"""INSERT INTO "RepeatedSchedule" (hostid, repeatdate, repeatperiod, start, "end", appointmentduration, place) VALUES """ ++ values.foldSmash(fr"", fr", ", fr""))
      .update
      .withGeneratedKeysWithChunkSize[RepeatedScheduleId]("id")(values.size)
      .compile.fold(List[ScheduleId]())((acc, id) => id :: acc)
  }

  def selectBeforeDate(date: LocalDate): ConnectionIO[List[RepeatedSchedule]] = {
    (selectSql ++ Fragments.whereAnd(fr"repeatDate < $date"))
      .query[RepeatedSchedule]
      .to[List]
  }

  def generateSchedules(): ConnectionIO[List[ScheduleId]] = {
    val dateLimit = LocalDate.now().plusDays(1)

    selectBeforeDate(dateLimit).flatMap {

      case Nil => Free.pure(Nil)

      case head :: tail =>
        val transformed = NonEmptyList.of(head, tail :_*).map(_.data)
        for {
          gen <- Schedule.updateBatch(transformed.flatMap { rsd =>
            val startDate = rsd.repeatDate.plus(rsd.repeatPeriod)
            generateSchedules(rsd, startDate, dateLimit, NonEmptyList.of(generateSchedule(rsd, startDate)))
          })
          _ <- updateBatch(transformed.map(rsd =>
            rsd.copy(repeatDate = getNewRepeatDate(rsd.repeatDate, rsd.repeatPeriod, dateLimit)))
          )
        } yield gen
    }
  }

  private def generateSchedules(rsd: RepeatedScheduleData,
                                currentDate: LocalDate,
                                dateLimit: LocalDate,
                                acc: NonEmptyList[ScheduleData]): NonEmptyList[ScheduleData] = {

    if (currentDate.isBefore(dateLimit)) {
      generateSchedules(
        rsd,
        currentDate.plus(rsd.repeatPeriod),
        dateLimit,
        generateSchedule(rsd, currentDate.plus(rsd.repeatPeriod)) :: acc
      )
    } else {
      acc.reverse //todo ?
    }
  }

  private def generateSchedule(rsd: RepeatedScheduleData, date: LocalDate): ScheduleData = {
    rsd.into[ScheduleData].withFieldConst(_.date, date).transform
  }

  private def getNewRepeatDate(currentDate: LocalDate, repeatPeriod: Period, startDate: LocalDate): LocalDate = {
    if (currentDate.isBefore(startDate)) {
      getNewRepeatDate(currentDate.plus(repeatPeriod), repeatPeriod, startDate)
    } else {
      currentDate
    }
  }
}

case class RepeatedSchedule(id: ScheduleId, data: RepeatedScheduleData) extends IdEntity[ScheduleId, RepeatedScheduleData]

case class RepeatedScheduleData(hostId: UserId,
                                repeatDate: LocalDate,
                                repeatPeriod: Period,
                                start: LocalTime,
                                end: LocalTime,
                                appointmentDuration: Period,
                                place: String)

