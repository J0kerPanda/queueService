package db.data

import cats.data.NonEmptyList
import cats.free.Free
import db.DatabaseFormats._
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.scalaland.chimney.dsl._
import org.joda.time.{LocalDate, Period}

object RepeatedSchedule {

  type RepeatedScheduleId = Int

  private val selectSql = sql"""SELECT id, hostid, repeatdate, repeatperiod, appointmentintervals, appointmentduration, place FROM "RepeatedSchedule" """

  private val insertSql = sql"""INSERT INTO "RepeatedSchedule" (hostid, repeatdate, repeatperiod, appointmentintervals, appointmentduration, place) VALUES """

  private val updateSql = sql"""UPDATE "RepeatedSchedule" AS RS """

  def insert(s: RepeatedScheduleData): ConnectionIO[RepeatedScheduleId] = {
    (insertSql ++ fr"(${s.hostId}, ${s.repeatDate}, ${s.repeatPeriod}, ${s.appointmentIntervals}::timerange[], ${s.appointmentDuration}, ${s.place})")
      .update
      .withUniqueGeneratedKeys("id")
  }

  def insertBatch(ss: NonEmptyList[RepeatedScheduleData]):  ConnectionIO[List[RepeatedScheduleId]] = {
    val values = ss
      .map(s =>
        fr"(${s.hostId}, ${s.repeatDate}, ${s.repeatPeriod}, ${s.appointmentIntervals}::timerange[], ${s.appointmentDuration}, ${s.place})"
      )

    (insertSql ++ values.foldSmash(fr"", fr", ", fr""))
      .update
      .withGeneratedKeysWithChunkSize[RepeatedScheduleId]("id")(values.size)
      .compile.fold(List[ScheduleId]())((acc, id) => id :: acc)
  }

  def updateRepeatDates(ss: NonEmptyList[(RepeatedScheduleId, LocalDate)]):  ConnectionIO[Int] = {
    val values = ss.map(s => fr"(${s._1}, ${s._2}::DATE)")

    (updateSql
      ++ fr"SET repeatdate = C.repeatdate FROM " ++ values.foldSmash(fr"(VALUES ", fr", ", fr") AS C(id, repeatDate)" )
      ++ fr"WHERE RS.id = C.id"
    )
      .update
      .run
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
        val transformed = NonEmptyList.of(head, tail :_*)
        for {
          gen <- Schedule.insertBatch(transformed.flatMap { rs =>
            val startDate = rs.data.repeatDate.plus(rs.data.repeatPeriod)
            generateSchedules(rs, startDate, dateLimit, NonEmptyList.of(generateSchedule(rs, startDate)))
          })
          _ <- updateRepeatDates(
            transformed.map(rs => (rs.id, getNewRepeatDate(rs.data.repeatDate, rs.data.repeatPeriod, dateLimit)))
          )
        } yield gen
    }
  }

  private def generateSchedules(rs: RepeatedSchedule,
                                currentDate: LocalDate,
                                dateLimit: LocalDate,
                                acc: NonEmptyList[ScheduleData]): NonEmptyList[ScheduleData] = {

    if (currentDate.isBefore(dateLimit)) {
      generateSchedules(
        rs,
        currentDate.plus(rs.data.repeatPeriod),
        dateLimit,
        generateSchedule(rs, currentDate.plus(rs.data.repeatPeriod)) :: acc
      )
    } else {
      acc.reverse //todo ?
    }
  }

  private def generateSchedule(rs: RepeatedSchedule, date: LocalDate): ScheduleData = {
    rs.data.into[ScheduleData]
      .withFieldConst(_.date, date)
      .withFieldConst(_.repeatId, Some(rs.id))
      .transform
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
                                appointmentIntervals: List[AppointmentInterval],
                                appointmentDuration: Period,
                                place: String)

