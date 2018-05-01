package controllers

import controllers.formats.HttpFormats._
import controllers.formats.UserInputData
import db.ConnectionUtils
import db.data.User
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsObject
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class UserController @Inject()(cu: ConnectionUtils, cc: ControllerComponents) extends AbstractController(cc) {

  def register = Action { request =>

    request.body.asJson match {

      case Some(json: JsObject) =>
        val inputData = json.as[UserInputData]
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

      case json => Responses.badJson(json.toString)
    }
  }

  def get(id: Int) = Action {
    Ok(User.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }

  def test = Action {
    Ok("")
  }
}