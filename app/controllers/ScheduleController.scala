package controllers

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.ActionBuilders
import cats.free.Free
import controllers.auth.{AuthUser, Roles}
import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.request.CreateScheduleRequest
import controllers.formats.response.{GenericScheduleFormat, ScheduleListDataFormat}
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

import scala.concurrent.ExecutionContext

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

      (for {
        _ <- Schedule.blockRepeatedByDate(sd.date)
        _ <- Schedule.insert(
          sd.into[ScheduleData]
            .withFieldConst(_.hostId, user.id)
            .withFieldConst(_.repeatId, None)
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

  def update: Action[AnyContent] = ab.RestrictAction(Roles.Host.name).defaultHandler() { implicit r =>
    extractJsObjectAsync[GenericScheduleFormat] { gs =>
      val user = r.subject.get.asInstanceOf[AuthUser]

      Schedule.select(gs.id)
        .flatMap[Option[ScheduleListDataFormat]] {
          case Some(s) if s.data.hostId == user.id =>
            val updated = s.data.copy(
              appointmentIntervals = gs.appointmentIntervals,
              appointmentDuration = gs.appointmentDuration,
              place = gs.place,
              isBlocked = gs.isBlocked
            )

            for {
              _ <- Schedule.update(Schedule(gs.id, updated))
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

  def delete(scheduleId: ScheduleId): Action[AnyContent] = ab.RestrictAction(Roles.Host.name).defaultHandler() { r =>
    val user = r.subject.get.asInstanceOf[AuthUser]

    Schedule.select(scheduleId)
      .flatMap[Int] {
        case Some(s) if (s.id == scheduleId) && (s.data.hostId == user.id) => Schedule.delete(scheduleId)
        case _ => Free.pure(0)
      }
      .flatMap[Option[ScheduleListDataFormat]] {
        case 0 => Free.pure(None)
        case _ => selectSchedules(scheduleId)
      }
      .map {
        case Some(res) => Ok(res.toJson)
        case None => BadRequest
      }
      .transact(cu.transactor)
      .unsafeToFuture()
  }

  def createRepeated: Action[AnyContent] = ab.RestrictAction(Roles.Host.name).defaultHandler() { implicit r =>
    extractJsObjectAsync[RepeatedScheduleData] { sd =>
      RepeatedSchedule
        .insert(sd)
        .transact(cu.transactor)
        .attempt
        .unsafeToFuture()
        .map {

        case Left(err) =>
          Logger.error("schedule error", err)
          ErrorResponses.invalidScheduleData

        case Right(id) => Created(id.toString)
      }
    }
  }

  def getSchedules(hostId: UserId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {

    selectSchedules(hostId)
      .transact(cu.transactor)
      .unsafeToFuture()
      .map(_.map(res => Ok(res.toJson)).getOrElse(ErrorResponses.invalidHostUser(hostId)))
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
                s => s.data.date -> s.data.into[GenericScheduleFormat].withFieldConst(_.id, s.id).transform
              ).toMap
            ))
          )
    }
  }
}