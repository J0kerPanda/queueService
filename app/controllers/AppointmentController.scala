package controllers

import java.util.Date

import controllers.HttpFormats._
import db.ConnectionUtils
import db.data.Appointment
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class AppointmentController @Inject()(cc: ControllerComponents, cu: ConnectionUtils) extends AbstractController(cc) {

  def create(hostId: Int, visitorId: Int) = Action {
    val appointment = Appointment.forInsertion(hostId, visitorId, DateTime.now())

    val tr = for {
      id <- Appointment.insert(appointment)
      a <- Appointment.selectById(id)
    } yield a

    Created(tr.transact(cu.transactor).unsafeRunSync().toJson)
  }

  def get(id: Long) = Action {
    Ok(Appointment.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }
}
