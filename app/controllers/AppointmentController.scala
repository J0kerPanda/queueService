package controllers

import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.request.CreateAppointmentRequest
import controllers.util.ControllerUtils.extractJsObject
import db.ConnectionUtils
import db.data.Appointment
import db.data.User.UserId
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class AppointmentController @Inject()(cc: ControllerComponents, cu: ConnectionUtils) extends AbstractController(cc) {

  //todo unique constraint errors

  def create = Action { implicit r =>
    extractJsObject[CreateAppointmentRequest] { req =>

      if (req.hostId == req.visitorId) {
        ErrorResponses.invalidHostUser(req.hostId)
      } else {
        Appointment.insert(req.toAppointmentData())
          .transact(cu.transactor)
          .attempt
          .unsafeRunSync() match {

            case Left(e) =>
              println(e)
              BadRequest //todo error

            case Right(d) => Ok
        }
      }
    }
  }

  def get(id: Long) = Action {
    Ok(Appointment.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }

  def byDate(hostId: UserId, date: LocalDate) = Action {
    Ok(
      Appointment.selectByDate(hostId, date)
        .transact(cu.transactor)
        .unsafeRunSync()
        .toJson
    )
  }
}
