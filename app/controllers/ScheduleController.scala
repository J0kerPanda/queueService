package controllers

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.ActionBuilders
import cats.data.NonEmptyList
import cats.free.Free
import controllers.auth.{AuthUser, Roles}
import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.request.{CreateRepeatedScheduleRequest, CreateScheduleRequest}
import controllers.formats.response.{GenericRepeatedScheduleFormat, GenericScheduleFormat, RepeatedScheduleListDataFormat, ScheduleListDataFormat}
import controllers.util.ControllerUtils
import controllers.util.ControllerUtils._
import db.DbConnectionUtils
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import db.data._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.scalaland.chimney.dsl._
import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ScheduleController @Inject()(ab: ActionBuilders,
                                   bp: PlayBodyParsers,
                                   cu: DbConnectionUtils,
                                   cc: ControllerComponents,
                                   system: ActorSystem) extends AbstractController(cc) {

  private implicit val _bp: PlayBodyParsers = bp
  private implicit val _ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  def create: Action[AnyContent] = ab.RestrictAction(Roles.Host.name).defaultHandler() { implicit r =>
    extractJsObjectAsync[CreateScheduleRequest] { sd =>
      val user = r.subject.get.asInstanceOf[AuthUser]

      if (sd.appointmentIntervals.isEmpty) {
        Future.successful(BadRequest)
      } else {
        (for {
          _ <- Schedule.insert(
            sd.into[ScheduleData]
              .withFieldConst(_.hostId, user.id)
              .withFieldConst(_.repeatId, None)
              .withFieldConst(_.appointmentIntervals, mergeAppointments(sd.appointmentIntervals))
              .transform
          )
          schedules <- selectSchedules(user.id)
        } yield schedules)
          .transact(cu.transactor)
          .attempt
          .unsafeToFuture()
          .map {

            case Left(err) =>
              Logger.error("schedule error", err)
              ErrorResponses.invalidScheduleData

            case Right(data) => Ok(data.toJson)
          }
      }
    }
  }

  def getSchedules(hostId: UserId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    selectSchedules(hostId)
      .transact(cu.transactor)
      .unsafeToFuture()
      .map(_.map(res => Ok(res.toJson)).getOrElse(ErrorResponses.invalidHostUser(hostId)))
  }

  def update: Action[AnyContent] = ab.RestrictAction(Roles.Host.name).defaultHandler() { implicit r =>
    extractJsObjectAsync[GenericScheduleFormat] { gs =>
      val user = r.subject.get.asInstanceOf[AuthUser]

      if (gs.appointmentIntervals.isEmpty) {
        Future.successful(BadRequest)
      } else {

        Schedule.select(gs.id)
          .flatMap[Option[ScheduleListDataFormat]] {
          case Some(s) if s.data.hostId == user.id && gs.appointmentIntervals.nonEmpty =>
            val updated = s.data.copy(
              repeatId = None,
              appointmentIntervals = mergeAppointments(gs.appointmentIntervals),
              appointmentDuration = gs.appointmentDuration,
              place = gs.place
            )

            for {
              _ <- Schedule.update(Schedule(gs.id, updated))
              _ <- Appointment.deleteOutOfTimeAppointments(gs.id, updated.appointmentIntervals)
              schedules <- selectSchedules(user.id)
            } yield schedules

          case _ => Free.pure(None)
        }
          .map {
            case Some(res) => Ok(res.toJson)

            case None => BadRequest
          }
          .transact(cu.transactor)
          .unsafeToFuture()
      }
    }
  }

  def delete(scheduleId: ScheduleId): Action[AnyContent] = ab.RestrictAction(Roles.Host.name).defaultHandler() { r =>
    val user = r.subject.get.asInstanceOf[AuthUser]

    Schedule.select(scheduleId)
      .flatMap[Int] {
        case Some(s) if (s.id == scheduleId) && (s.data.hostId == user.id) => Schedule.delete(scheduleId)
        case _ => Free.pure(0)
      }
      .flatMap[Option[ScheduleListDataFormat]] {
        case 0 => Free.pure(None)
        case _ => selectSchedules(user.id)
      }
      .map {
        case Some(res) =>  Ok(res.toJson)
        case None => BadRequest
      }
      .transact(cu.transactor)
      .unsafeToFuture()
  }

  def createRepeated: Action[AnyContent] = ab.RestrictAction(Roles.Host.name).defaultHandler() { implicit r =>
    //todo check repeated date
    extractJsObjectAsync[CreateRepeatedScheduleRequest] { sd =>
      val user = r.subject.get.asInstanceOf[AuthUser]

      if (sd.appointmentIntervals.isEmpty) {
        Future.successful(BadRequest)
      } else {
        (for {
          _ <- RepeatedSchedule.insert(
            sd.into[RepeatedScheduleData]
              .withFieldConst(_.hostId, user.id)
              .withFieldConst(_.appointmentIntervals, mergeAppointments(sd.appointmentIntervals))
              .transform
          )
          _ <- RepeatedSchedule.generateSchedules()
          res <- selectRepeatedSchedules(user.id)
        } yield res)
          .transact(cu.transactor)
          .attempt
          .unsafeToFuture()
          .map {

            case Left(err) =>
              Logger.error("schedule error", err)
              ErrorResponses.invalidScheduleData

            case Right(res) => Ok(res.toJson)
          }
      }
    }
  }

  def getRepeatedSchedules(hostId: UserId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    selectRepeatedSchedules(hostId)
      .transact(cu.transactor)
      .unsafeToFuture()
      .map(res => Ok(res.toJson))
  }

  def deleteRepeated(scheduleId: ScheduleId): Action[AnyContent] = ab.RestrictAction(Roles.Host.name).defaultHandler() { r =>
    val user = r.subject.get.asInstanceOf[AuthUser]

    RepeatedSchedule.select(scheduleId)
      .flatMap[Int] {
        case Some(s) if (s.id == scheduleId) && (s.data.hostId == user.id) => RepeatedSchedule.delete(scheduleId)
        case _ => Free.pure(0)
      }
      .flatMap[Option[RepeatedScheduleListDataFormat]] {
        case 0 => Free.pure(None)
        case _ => for {
          _ <- Schedule.removeRepeatId(scheduleId)
          res <- selectRepeatedSchedules(user.id).map(Some(_))
        } yield res
      }
      .map {
        case Some(res) => Ok(res.toJson)
        case None => BadRequest
      }
      .transact(cu.transactor)
      .unsafeToFuture()
  }

  private def selectSchedules(hostId: UserId): ConnectionIO[Option[ScheduleListDataFormat]] = {
    HostMeta.selectById(hostId)
      .flatMap[Option[ScheduleListDataFormat]] {
        case None => Free.pure(None)

        case Some(hm) =>
          val from = LocalDate.now()
          val to = from.plus(hm.appointmentPeriod.toStandardDays)
          Schedule
            .selectInPeriod(hostId, from, to)
            .map(schedules =>
              Some(ScheduleListDataFormat(
                hostId = hostId,
                period = hm.appointmentPeriod,
                schedules = schedules.map(
                  s => s.data.date -> s.data.into[GenericScheduleFormat]
                    .withFieldConst(_.id, s.id)
                    .withFieldConst(_.appointmentIntervals, s.data.appointmentIntervals.toList)
                    .transform
                ).toMap
              ))
            )
    }
  }

  private def selectRepeatedSchedules(hostId: UserId): ConnectionIO[RepeatedScheduleListDataFormat] = {
    RepeatedSchedule.selectByHostId(hostId)
      .map(res => RepeatedScheduleListDataFormat(
        hostId = hostId,
        schedules = res.map(
          s => s.data.repeatDate -> s.data.into[GenericRepeatedScheduleFormat]
            .withFieldConst(_.id, s.id)
            .withFieldConst(_.appointmentIntervals, s.data.appointmentIntervals.toList)
            .transform
        ).toMap
      ))
  }

  private def mergeAppointments(appointmentIntervals: List[AppointmentInterval]): NonEmptyList[AppointmentInterval] = {
    val sorted = appointmentIntervals.sortWith((e1, e2) => e1.start.isBefore(e2.start))

    sorted.tail.foldLeft(NonEmptyList.of(sorted.head)) { (acc, el) =>
      if (el.start.isBefore(acc.head.end)) {
        NonEmptyList.of(acc.head.copy(end = el.end), acc.tail :_*)
      } else {
        el :: acc
      }
    }
  }
}