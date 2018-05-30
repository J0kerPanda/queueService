package controllers

import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.request.{LoginRequest, RegistrationRequest}
import controllers.formats.response.HostDataFormat
import controllers.util.AuthUtils._
import controllers.util.ControllerUtils._
import db.DbConnectionUtils
import db.data.User.UserId
import db.data.{HostMeta, User, UserData}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.Period
import play.api.mvc.{AbstractController, ControllerComponents}
import io.scalaland.chimney.dsl._

@Singleton
class UserController @Inject()(cu: DbConnectionUtils, cc: ControllerComponents) extends AbstractController(cc) {

  //todo unique constraint errors
  def login = Action { implicit r =>
    extractJsObject[LoginRequest] { ld =>

      User.login(ld.email, ld.password)
        .transact(cu.transactor)
        .unsafeRunSync() match {

          case Some(user) => Ok(user.toJson).addSession(user.id)

          case None => ErrorResponses.loginFailed
      }
    }
  }

  def register = Action { implicit request =>
    extractJsObject[RegistrationRequest] { inputData =>

      val user = UserData(
        firstName = inputData.firstName,
        surname = inputData.surname,
        patronymic = inputData.patronymic,
        password = inputData.password,
        email = inputData.email
      )

      val tr: ConnectionIO[Option[User]] = for {
        id <- User.insert(user)
        u <- User.selectById(id)
      } yield u

      Created(tr.transact(cu.transactor).unsafeRunSync().toJson)
    }
  }

  def promote(id: UserId) = Action {
    val tr: ConnectionIO[UserId] = for {
      _ <- User.promote(id)
      r <- HostMeta.insert(HostMeta(id, Period.days(31)))
    } yield r

    Ok(tr.transact(cu.transactor).unsafeRunSync().toString)
  }

  def get(id: Int) = Action {
    Ok(User.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }

  def getHosts = Action {
    Ok(User.selectHosts()
      .transact(cu.transactor)
      .unsafeRunSync()
      .map(u => u.data.into[HostDataFormat].withFieldConst(_.id, u.id).transform)
      .toJson
    )
  }

  def test = Action {
    Ok("")
  }
}