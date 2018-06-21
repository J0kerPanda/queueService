package db.data

import cats.data.NonEmptyList
import cats.free.Free
import db.DatabaseFormats._
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie.free.connection.{ConnectionIO, ConnectionOp}
import doobie.implicits._
import io.scalaland.chimney.dsl._
import org.joda.time.{LocalDate, Period}

object RepeatedSchedule {

  type RepeatedScheduleId = Int

  private val selectSql = sql"""SELECT id, hostid, repeatdate, repeatperiod, appointmentintervals, appointmentduration, place FROM "RepeatedSchedule" """

  private val insertSql = sql"""INSERT INTO "RepeatedSchedule" (hostid, repeatdate, repeatperiod, appointmentintervals, appointmentduration, place) VALUES """

  private val updateSql = sql"""UPDATE "RepeatedSchedule" AS RS """

  private val deleteSql = sql"""DELETE FROM "RepeatedSchedule" """

  def insert(s: RepeatedScheduleData): ConnectionIO[RepeatedScheduleId] = {
    (insertSql ++ fr"(${s.hostId}, ${s.repeatDate}, ${s.repeatPeriod}, ${s.appointmentIntervals}::timerange[], ${s.appointmentDuration}, ${s.place})")
      .update
      .withUniqueGeneratedKeys("id")
  }

  def selectByHostId(hostId: UserId): ConnectionIO[List[RepeatedSchedule]] = {
    (selectSql ++ fr"WHERE hostId = $hostId")
      .query[RepeatedSchedule]
      .to[List]
  }

  def delete(id: ScheduleId): ConnectionIO[Int] = {
    (deleteSql ++ fr"WHERE id = $id")
      .update
      .run
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

  def generateSchedules(): ConnectionIO[List[ScheduleId]] = {
    selectSql.query[RepeatedSchedule].to[List].flatMap {

      case Nil => Free.pure(Nil)

      case head :: tail =>
        val repeatedSchedules = NonEmptyList.of(head, tail :_*)
        for {
          dateLimits <- HostMeta
            .selectByIds(repeatedSchedules.map(_.data.hostId))
            .map(_.map(m => m.id -> LocalDate.now().plus(m.appointmentPeriod)).toMap)
          excludedDates <- Schedule.selectOccupiedDates(LocalDate.now())
              .map(_.groupBy(_._1)
                .map {
                  case (k, v) => k -> v.map(_._2).toSet
                }
                .toMap
              )
          gen <- Free.pure[ConnectionOp, NonEmptyList[RepeatedSchedule]](repeatedSchedules)
            .map(
              _.flatMap { rs =>
                  val startDate = rs.data.repeatDate.plus(rs.data.repeatPeriod)
                  generateSchedules(rs, startDate, dateLimits(rs.data.hostId), NonEmptyList.of(generateSchedule(rs, startDate)))
                }
                .filter(d => !excludedDates(d.hostId).contains(d.date))
            )
            .flatMap[List[ScheduleId]] {
              case h :: t => Schedule.insertBatch(NonEmptyList.of(h, t :_*))
              case _ => Free.pure(Nil)
            }
          _ <- updateRepeatDates(
            repeatedSchedules
              .map(rs => (rs.id, getNewRepeatDate(rs.data.repeatDate, rs.data.repeatPeriod, dateLimits(rs.data.hostId))))
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

