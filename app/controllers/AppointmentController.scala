package controllers

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.ActionBuilders
import cats.free.Free
import controllers.auth.AuthUser
import controllers.formats.HttpFormats._
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

import scala.concurrent.ExecutionContext

@Singleton
class AppointmentController @Inject()(ab: ActionBuilders,
                                      bp: PlayBodyParsers,
                                      cc: ControllerComponents,
                                      cu: DbConnectionUtils,
                                      system: ActorSystem) extends AbstractController(cc) {

  private implicit val _bp: PlayBodyParsers = bp
  private implicit val _ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  //todo unique constraint errors

  def create: Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() { implicit r =>
    extractJsObjectAsync[AppointmentData] { req =>

      Appointment.insert(req)
        .transact(cu.transactor)
        .attempt
        .unsafeToFuture()
        .map {

          case Left(e) =>
            println(e)
            BadRequest //todo error

          case Right(_) => Ok
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

  def cancel(id: AppointmentId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() { implicit r =>
    Appointment.checkAppointmentUser(id, r.subject.get.asInstanceOf[AuthUser].id)
      .flatMap[Result] {
        case Some(true) => Appointment.delete(id).map(_ => Ok)

        case Some(false) => Free.pure(Forbidden)

        case None => Free.pure(NotFound)
      }
      .transact(cu.transactor)
      .unsafeToFuture()
  }

  def byVisitorId(id: UserId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    Appointment.selectByVisitorId(id, new LocalDate())
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
