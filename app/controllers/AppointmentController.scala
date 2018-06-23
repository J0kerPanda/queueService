package controllers

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.ActionBuilders
import cats.free.Free
import controllers.auth.AuthUser
import controllers.formats.HttpFormats._
import controllers.formats.request.CreateAppointmentRequest
import controllers.util.ControllerUtils
import controllers.util.ControllerUtils._
import db.DbConnectionUtils
import db.data.Appointment.AppointmentId
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import db.data.{Appointment, AppointmentData}
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.mvc._
import io.scalaland.chimney.dsl._

import scala.concurrent.ExecutionContext

@Singleton
class AppointmentController @Inject()(ab: ActionBuilders,
                                      bp: PlayBodyParsers,
                                      cc: ControllerComponents,
                                      cu: DbConnectionUtils,
                                      system: ActorSystem) extends AbstractController(cc) {

  private implicit val _bp: PlayBodyParsers = bp
  private implicit val _ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  def create: Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() { implicit r =>
    extractJsObjectAsync[CreateAppointmentRequest] { req =>
      val user = r.subject.get.asInstanceOf[AuthUser]

      Appointment
        .insert(req.into[AppointmentData].withFieldConst(_.visitorId, user.id).transform)
        .flatMap(_ => Appointment.selectByScheduleId(req.scheduleId))
        .transact(cu.transactor)
        .attempt
        .unsafeToFuture()
        .map {
          case Left(_) => BadRequest

          case Right(res) => Ok(res.toJson)
        }
    }
  }

  def get(id: AppointmentId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    Appointment.selectById(id)
      .transact(cu.transactor)
      .map { r =>
        Ok(r.toJson).withSession(new Session())
      }
      .unsafeToFuture()
  }

  def complete(id: AppointmentId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() { implicit r =>
    val user = r.subject.get.asInstanceOf[AuthUser]

    Appointment.selectById(id)
      .flatMap[Result] {
        case Some(a) if a.data.visitorId == user.id =>
          Appointment
            .complete(id)
            .flatMap(_ => Appointment.selectByScheduleId(a.data.scheduleId))
            .map(r => Ok(r.toJson))

        case None => Free.pure(BadRequest)
      }
      .transact(cu.transactor)
      .unsafeToFuture()
  }

  def cancel(id: AppointmentId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() { implicit r =>
    val user = r.subject.get.asInstanceOf[AuthUser]

    Appointment.selectById(id)
      .flatMap[Result] {
        case Some(a) if a.data.visitorId == user.id =>
          Appointment
            .delete(id)
            .flatMap(_ => Appointment.selectByScheduleId(a.data.scheduleId))
            .map(r => Ok(r.toJson))

        case None => Free.pure(BadRequest)
      }
      .transact(cu.transactor)
      .unsafeToFuture()
  }

  def byVisitorId(id: UserId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    Appointment.selectByVisitorId(id, LocalDate.now())
      .transact(cu.transactor)
      .unsafeToFuture()
      .map(r => Ok(r.toJson))
  }

  def byScheduleId(id: ScheduleId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    Appointment.selectByScheduleId(id)
      .transact(cu.transactor)
      .unsafeToFuture()
      .map(r => Ok(r.toJson))
  }
}
