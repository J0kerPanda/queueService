package controllers

import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import doobie.implicits._
import controllers.util.ControllerUtils._
import db.ConnectionUtils
import db.data.{CustomScheduleData, DefaultScheduleData, Schedule}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class ScheduleController @Inject()(cu: ConnectionUtils, cc: ControllerComponents) extends AbstractController(cc) {

  //todo unique constraint errors
  def createDefault = Action { request =>
    extractJsObject[DefaultScheduleData](request) { sd =>
      val schedule = DefaultScheduleData(sd.hostId, sd.day, sd.start, sd.end, sd.interval, sd.place)

      Schedule
        .insertDefault(schedule)
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
      val schedule = CustomScheduleData(sd.hostId, sd.date, sd.start, sd.end, sd.interval, sd.place)

      Schedule
        .insertCustom(schedule)
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

  //todo add default, remove default -> composition
}