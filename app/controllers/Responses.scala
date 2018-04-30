package controllers

import db.data.Category.CategoryId
import play.api.libs.json.Writes
import HttpFormats._
import play.api.mvc.Results._

case class OkResponse[T](entity: Option[T])(implicit w: Writes[T])

case class ErrorResponse(msg: String, source: scala.Option[String])

object Responses {

  def emailExists(email: String) =
    Conflict(ErrorResponse(msg = s"with email $email already exists", source = Some("email")).toJson)

  def googleIdExists(googleId: String) =
    Conflict(ErrorResponse(s"google id $googleId already exists", source = Some("googleId")).toJson)

  def invalidCategory(categoryId: CategoryId) =
    BadRequest(ErrorResponse(s"invalid category id $categoryId", source = Some("categoryId")).toJson)

  def fieldIsTooLong(field: String, length: Int = 255) =
    BadRequest(ErrorResponse(s"field $field is too long (max $length)", source = Some(field)).toJson)

  def badJson(json: String) = BadRequest(ErrorResponse(s"bad json", source = Some(json)).toJson)

  def invalidFields(fields: List[String]) =
    BadRequest(ErrorResponse(s"invalid fields", source = Some(s"[${fields.mkString(", ")}]")).toJson)
}
