package controllers

import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.request.{AppointmentsRequest, CreateAppointmentRequest}
import controllers.formats.response.ScheduleData
import controllers.util.ControllerUtils.extractJsObject
import db.ConnectionUtils
import db.data.{Appointment, AppointmentData}
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class AppointmentController @Inject()(cc: ControllerComponents, cu: ConnectionUtils) extends AbstractController(cc) {

  //todo unique constraint errors

//  def create = Action { implicit r =>
//    extractJsObject[CreateAppointmentRequest] { req =>
//
//      if (req.hostId == req.visitorId) {
//        ErrorResponses.invalidHostUser(req.hostId)
//      } else {
//
//        //todo check date for schedules?
//
//        Appointment.createBySchedule(req.hostId, req.visitorId, req.scheduleId, req.isCustom, req.date, req.start)
//          .transact(cu.transactor)
//          .unsafeRunSync() match {
//
//            case Some(_) => Created
//
//            case None => BadRequest //todo error
//        }
//      }
//    }
//  }

  def create = Action { implicit r =>
    extractJsObject[CreateAppointmentRequest] { req =>

      if (req.hostId == req.visitorId) {
        ErrorResponses.invalidHostUser(req.hostId)
      } else {
        Appointment.insert(req.toAppointmentData())
          .transact(cu.transactor)
          .attempt
          .unsafeRunSync() match {

            case Left(e) => BadRequest //todo error

            case Right(d) => Ok
        }
      }
    }
  }

  def get(id: Long) = Action {
    Ok(Appointment.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }

  def byDate = Action { implicit r =>
    extractJsObject[AppointmentsRequest] { req =>

      Ok(
        Appointment.selectGeneric(req.hostId, req.date, req.scheduleIds, req.isCustom)
          .transact(cu.transactor)
          .unsafeRunSync()
          .toJson
      )
    }
  }
}
