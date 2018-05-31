package controllers

import akka.actor.ActorSystem
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
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext
import io.scalaland.chimney.dsl._

@Singleton
class ScheduleController @Inject()(cu: DbConnectionUtils, cc: ControllerComponents, system: ActorSystem)
  extends AbstractController(cc) {

  private implicit val ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  def create = Action { implicit r =>
    extractJsObject[ScheduleData] { sd =>

      Schedule
        .insert(sd)
        .transact(cu.transactor)
        .attempt
        .unsafeRunSync() match {

        case Left(err) =>
          Logger.error("schedule error", err)
          ErrorResponses.invalidScheduleData

        case Right(id) => Created(id.toString)
      }
    }
  }

  def createRepeat = Action { implicit r =>
    extractJsObject[RepeatedScheduleData] { sd =>

      RepeatedSchedule
        .insert(sd)
        .transact(cu.transactor)
        .attempt
        .unsafeRunSync() match {

        case Left(err) =>
          Logger.error("schedule error", err)
          ErrorResponses.invalidScheduleData

        case Right(id) => Created(id.toString)
      }
    }
  }

  def getSchedules(hostId: UserId): Action[AnyContent] = Action {

    HostMeta.selectById(hostId).transact(cu.transactor).unsafeRunSync().map(_.appointmentPeriod)
      .map { period =>
        val from = new LocalDate()
        val to = from.plus(period.toStandardDays)
        Ok(ScheduleListDataFormat(
          hostId = hostId,
          period = period,
          schedules = Schedule
            .selectSchedules(hostId, from, to)
            .transact(cu.transactor)
            .unsafeRunSync()
            .map(_.data.into[GenericScheduleFormat].transform)
        ).toJson)
      }
      .getOrElse(ErrorResponses.invalidHostUser(hostId))
  }

  //todo add default, remove default -> composition
}