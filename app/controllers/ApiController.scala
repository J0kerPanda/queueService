package controllers

import cats.data.NonEmptyList
import controllers.HttpFormats._
import db.ConnectionUtils
import db.data.User
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class ApiController @Inject()(cu: ConnectionUtils, cc: ControllerComponents, ec: ExecutionContextProvider) extends AbstractController(cc) {

  private implicit val _ec: ExecutionContext = ec.get()

  def register: Action[AnyContent] = Action {
    val id: Int = System.currentTimeMillis().toInt
    println(id)
    val user = User(
      id = id.toInt,
      firstName = "test",
      surname = "test",
      lastName = "test",
      password = "test",
      email = s"$id@test.com",
      googleId = 2,
      categoryId = 1)

    val tr: ConnectionIO[List[User]] = for {
      _ <- User.insert(user)
      p <- User.selectByIds(NonEmptyList.of(id))
    } yield p

    Ok(tr.transact(cu.transactor).unsafeRunSync().toJson)
  }

  def getUser(id: Int) = Action {
    Ok(User.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }

  def test: Action[AnyContent] = Action {
    Ok(User.testSelect(NonEmptyList.of(282519388, 284456580)).transact(cu.transactor).unsafeRunSync().toString())
  }
}