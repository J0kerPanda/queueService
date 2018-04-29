package controllers

import java.util.Date

import db.{ConnectionUtils, DatabaseUtils}
import db.data.Appointment
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}
import doobie.implicits._
import HttpFormats._

@Singleton
class AppointmentController @Inject()(cc: ControllerComponents, cu: ConnectionUtils) extends AbstractController(cc) {

  def create(hostId: Int, visitorId: Int) = Action {
    val appointment = Appointment.forInsertion(hostId, visitorId, new Date())

    val tr = for {
      _ <- Appointment.insert(appointment)
      id <- DatabaseUtils.returnLastLongId
      a <- Appointment.selectById(id)
    } yield a

    Created(tr.transact(cu.transactor).unsafeRunSync().toJson)
  }

  def get(id: Long) = Action {
    Ok(Appointment.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }
}
