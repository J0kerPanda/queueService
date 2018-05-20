package controllers

import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.response.HostData
import controllers.formats.request.{LoginData, UserInputData}
import controllers.util.ControllerUtils._
import db.ConnectionUtils
import db.data.User.UserId
import db.data.{HostMeta, User, UserData}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.Period
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class UserController @Inject()(cu: ConnectionUtils, cc: ControllerComponents) extends AbstractController(cc) {

  //todo unique constraint errors
  def login = Action { implicit r =>
    extractJsObject[LoginData] { ld =>

      User.login(ld.email, ld.password)
        .transact(cu.transactor)
        .unsafeRunSync() match {

          case Some(user) =>
            Ok

          case None => ErrorResponses.loginFailed
      }
    }
  }

  def register = Action { implicit request =>
    extractJsObject[UserInputData] { inputData =>

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
      .map(user => HostData(user.id, user.data.firstName, user.data.surname, user.data.patronymic))
      .toJson
    )
  }

  def test = Action {
    Ok("")
  }
}