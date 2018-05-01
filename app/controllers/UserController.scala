package controllers

import controllers.formats.HttpFormats._
import controllers.formats.{LoginData, UserInputData}
import db.ConnectionUtils
import db.data.User
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import controllers.util.ControllerUtils._
import controllers.util.Responses
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class UserController @Inject()(cu: ConnectionUtils, cc: ControllerComponents) extends AbstractController(cc) {

  def login = Action { request =>
    extractJsObject[LoginData](request) { loginData =>

      User.login(loginData.email, loginData.password)
        .transact(cu.transactor)
        .unsafeRunSync() match {

          case Some(user) => Ok(user.toJson)

          case None => Responses.loginFailed()
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

      val tr: ConnectionIO[scala.Option[User]] = for {
        id <- User.insert(user)
        u <- User.selectById(id)
      } yield u

      Created(tr.transact(cu.transactor).unsafeRunSync().toJson)
    }
  }

  def get(id: Int) = Action {
    Ok(User.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }

  def test = Action {
    Ok("")
  }
}