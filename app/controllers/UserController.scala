package controllers

import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.{LoginData, UserInputData}
import controllers.util.ControllerUtils._
import db.ConnectionUtils
import db.data.User.UserId
import db.data.{HostMeta, User}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.Period
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.duration._

@Singleton
class UserController @Inject()(cu: ConnectionUtils, cc: ControllerComponents) extends AbstractController(cc) {

  //todo unique constraint errors
  def login = Action { request =>
    extractJsObject[LoginData](request) { loginData =>

      User.login(loginData.email, loginData.password)
        .transact(cu.transactor)
        .unsafeRunSync() match {

          case Some(user) => Ok(user.toJson)

          case None => ErrorResponses.loginFailed
      }
    }
  }

  def register = Action { request =>
    extractJsObject[UserInputData](request) { inputData =>

      val user = User.forInsertion(
        firstName = inputData.firstName,
        surname = inputData.surname,
        lastName = inputData.lastName,
        password = inputData.password,
        email = inputData.email,
        googleId = inputData.googleId,
        categoryId = inputData.categoryId
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

  def test = Action {
    Ok("")
  }
}