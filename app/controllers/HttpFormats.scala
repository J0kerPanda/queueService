package controllers

import db.data.User
import play.api.libs.json._

object HttpFormats {

  implicit val userJsonFormat: OFormat[User] = Json.format[User]
}
