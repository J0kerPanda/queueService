package controllers

import akka.actor.ActorSystem
import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.ScheduleData
import controllers.util.ControllerUtils
import controllers.util.ControllerUtils._
import db.ConnectionUtils
import db.data.User.UserId
import db.data.{CustomScheduleData, DefaultScheduleData, HostMeta, Schedule}
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.Logger
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class ScheduleController @Inject()(cu: ConnectionUtils, cc: ControllerComponents, system: ActorSystem)
  extends AbstractController(cc) {

  private implicit val ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  //todo unique constraint errors
  def createDefault = Action { request =>
    extractJsObject[DefaultScheduleData](request) { sd =>

      Schedule
        .insertDefault(sd)
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

  def createCustom = Action { request =>
    extractJsObject[CustomScheduleData](request) { sd =>

      Schedule
        .insertCustom(sd)
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

    val periodOpt = HostMeta.selectById(hostId).transact(cu.transactor).unsafeRunSync().map(_.appointmentPeriod)

    periodOpt.map { period =>
      val from = new LocalDate()
      val to = from.plus(period.toStandardDays)
      Ok(ScheduleData(
        hostId,
        period,
        Schedule.selectSchedules(hostId, from, to).transact(cu.transactor).unsafeRunSync()
      ).toJson)
    }
      .getOrElse(ErrorResponses.invalidHostUser(hostId))
  }

  //todo add default, remove default -> composition
}