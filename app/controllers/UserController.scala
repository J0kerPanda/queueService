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

  def register = Action {
    val id: Int = System.currentTimeMillis().toInt
    println(id)
    val user = User(
      id = id.toInt,
      firstName = "test",
      surname = "test",
      lastName = "test",
      password = "test",
      email = s"$id@test.com",
      googleId = id.toString,
      categoryId = 1)

    val tr: ConnectionIO[scala.Option[User]] = for {
      _ <- User.insert(user)
      p <- User.selectById(id)
    } yield p

    Ok(tr.transact(cu.transactor).unsafeRunSync().toJson)
  }

  def getUser(id: Int) = Action {
    Ok(User.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }

  def test = Action {
    Ok("")
  }
}