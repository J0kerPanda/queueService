package controllers

import controllers.HttpFormats._
import db.ConnectionUtils
import db.data.User
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class UserController @Inject()(cu: ConnectionUtils, cc: ControllerComponents) extends AbstractController(cc) {

  def register = Action { request =>
    val pseudoId: Long = System.currentTimeMillis()
    val user = User.forInsertion(
      firstName = "test",
      surname = "test",
      lastName = "test",
      password = "test",
      email = s"$pseudoId@test.com",
      googleId = pseudoId.toString,
      categoryId = 1)

    val tr: ConnectionIO[scala.Option[User]] = for {
      id <- User.insert(user)
      u <- User.selectById(id)
    } yield u

    Created(tr.transact(cu.transactor).unsafeRunSync().toJson)
  }

  def get(id: Int) = Action {
    Ok(User.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }

  def test = Action {
    Ok("")
  }
}