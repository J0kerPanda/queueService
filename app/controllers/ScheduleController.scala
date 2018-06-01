package controllers

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.ActionBuilders
import cats.free.Free
import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.response.{GenericScheduleFormat, ScheduleListDataFormat}
import controllers.util.ControllerUtils
import controllers.util.ControllerUtils._
import db.DbConnectionUtils
import db.data.User.UserId
import db.data._
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.Logger
import play.api.mvc._

import scala.concurrent.ExecutionContext
import io.scalaland.chimney.dsl._

@Singleton
class ScheduleController @Inject()(ab: ActionBuilders,
                                   bp: PlayBodyParsers,
                                   cu: DbConnectionUtils,
                                   cc: ControllerComponents,
                                   system: ActorSystem) extends AbstractController(cc) {

  private implicit val _bp: PlayBodyParsers = bp
  private implicit val _ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  def create: Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() { implicit r =>
    extractJsObjectAsync[ScheduleData] { sd =>
      //todo format -> remove repeatid

      val tr = for {
        _ <- Schedule.blockRepeatedByDate(sd.date)
        id <- Schedule.insert(sd)
      } yield id

      tr
        .transact(cu.transactor)
        .attempt
        .unsafeToFuture()
        .map {

        case Left(err) =>
          Logger.error("schedule error", err)
          ErrorResponses.invalidScheduleData

        case Right(id) => Created(id.toJson)
      }
    }
  }

  def createRepeated: Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() { implicit r =>
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

    HostMeta.selectById(hostId)
      .flatMap[Option[ScheduleListDataFormat]] {
        case None => Free.pure(None)

        case Some(hm) =>

          val from = new LocalDate()
          val to = from.plus(hm.appointmentPeriod.toStandardDays)
          Schedule
            .selectInPeriod(hostId, from, to)
            .map(schedules =>
              Some(ScheduleListDataFormat(
                hostId = hostId,
                period = hm.appointmentPeriod,
                schedules = schedules.map(
                  s => s.data.into[GenericScheduleFormat].withFieldConst(_.id, s.id).transform
                )
              ))
            )
      }
      .transact(cu.transactor)
      .unsafeToFuture()
      .map(_.map(res => Ok(res.toJson)).getOrElse(ErrorResponses.invalidHostUser(hostId)))
  }

  //todo add default, remove default -> composition
}