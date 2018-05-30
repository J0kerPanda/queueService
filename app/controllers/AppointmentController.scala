package controllers

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.ActionBuilders
import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.util.ControllerUtils
import controllers.util.ControllerUtils._
import db.DbConnectionUtils
import db.data.{Appointment, AppointmentData}
import db.data.User.UserId
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

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
    Future(extractJsObjectAuth[AppointmentData] { req =>

      Appointment.insert(req)
        .transact(cu.transactor)
        .attempt
        .unsafeRunSync() match {

          case Left(e) =>
            println(e)
            BadRequest //todo error

          case Right(_) => Ok
      }
    })
  }

  def get(id: Long): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    Appointment.selectById(id)
      .transact(cu.transactor)
      .map { r =>
        Ok(r.toJson).withSession(new Session())
      }
      .unsafeToFuture()
  }

  def byDate(hostId: UserId, date: LocalDate): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
      Appointment.selectByDate(hostId, date)
        .transact(cu.transactor)
        .unsafeToFuture()
        .map(r => Ok(r.toJson))
  }
}
