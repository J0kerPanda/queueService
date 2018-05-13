package controllers

import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.request.AppointmentsRequest
import db.ConnectionUtils
import db.data.User.UserId
import db.data.{Appointment, AppointmentData}
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.mvc.{AbstractController, ControllerComponents}
import controllers.util.ControllerUtils.extractJsObject

@Singleton
class AppointmentController @Inject()(cc: ControllerComponents, cu: ConnectionUtils) extends AbstractController(cc) {

  //todo unique constraint errors

  def create(hostId: UserId, visitorId: UserId) = Action {
    if (hostId == visitorId) {
      ErrorResponses.invalidHostUser(hostId)
    } else {
      val dateTime = DateTime.now()
      val date = dateTime.toLocalDate
      val start = dateTime.toLocalTime
      val end = start.plusHours(3)
      val appointment = AppointmentData(hostId, visitorId, date, start, end)

      val tr = for {
        id <- Appointment.insert(appointment)
        a <- Appointment.selectById(id)
      } yield a

      Created(tr.transact(cu.transactor).unsafeRunSync().toJson)
    }
  }

  def get(id: Long) = Action {
    Ok(Appointment.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }

  def byDate = Action { implicit request =>
    extractJsObject[AppointmentsRequest] { req =>

      Ok(
        Appointment.selectByDate(req.hostId, req.date, req.scheduleIds, req.isCustom)
          .transact(cu.transactor)
          .unsafeRunSync()
          .toJson
      )
    }
  }
}
