package controllers.util

import db.data.User.UserId
import play.api.mvc.{Result, Session}

object AuthUtils {

  val UserIdKey: String = "userId"

  implicit class ResultExtension(result: Result) {

    def addSession(hostId: UserId): Result = result.withSession(createSession(hostId))
  }

  def createSession(hostId: UserId): Session = Session(Map(UserIdKey -> hostId.toString))
}
