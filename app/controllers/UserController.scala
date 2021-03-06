package controllers

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.ActionBuilders
import controllers.auth.Roles
import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.request.LoginRequest
import controllers.formats.response.HostDataFormat
import controllers.util.AuthUtils._
import controllers.util.ControllerUtils
import controllers.util.ControllerUtils._
import db.DbConnectionUtils
import db.data.User.UserId
import db.data._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.scalaland.chimney.dsl._
import javax.inject.{Inject, Singleton}
import org.joda.time.Period
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject()(ab: ActionBuilders,
                               bp: PlayBodyParsers,
                               cu: DbConnectionUtils,
                               cc: ControllerComponents,
                               system: ActorSystem) extends AbstractController(cc) {

  private implicit val _bp: PlayBodyParsers = bp
  private implicit val _ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  def login: Action[AnyContent] = Action.async { implicit r =>
    extractJsObjectAsync[LoginRequest] { lr =>

      User.login(lr.email, lr.password)
        .transact(cu.transactor)
        .unsafeToFuture()
        .map {

          case Some(user) if !user.data.isBlocked =>
            Ok(user.toJson).addSession(user.id)

          case None => ErrorResponses.loginFailed
      }
    }
  }

  def promote(id: UserId): Action[AnyContent] = ab.RestrictAction(Roles.Host.name).defaultHandler() {
    val tr: ConnectionIO[UserId] = for {
      _ <- User.promote(id)
      r <- HostMeta.insert(HostMeta(id, Period.days(31)))
    } yield r

    tr.transact(cu.transactor).unsafeToFuture().map(_ => Ok)
  }

  def getHosts: Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    User.selectHosts()
      .transact(cu.transactor)
      .unsafeToFuture()
      .map(users => Ok(users.map(u => u.data.into[HostDataFormat].withFieldConst(_.id, u.id).transform).toJson))
  }

  def test = Action {
    RepeatedSchedule.generateSchedules().transact(cu.transactor).unsafeRunSync()
    Ok
  }
}