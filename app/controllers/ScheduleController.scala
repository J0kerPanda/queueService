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

      val tr = for {
        _ <- Schedule.blockRepeatedByDate(sd.date)
        _ <- Schedule.insert(
          sd.into[ScheduleData]
            .withFieldConst(_.hostId, user.id)
            .withFieldConst(_.repeatId, None)
            .transform
        )
        schedules <- selectSchedules(user.id)
      } yield schedules

      tr
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