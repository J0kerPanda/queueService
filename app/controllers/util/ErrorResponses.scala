package controllers.util

import controllers.formats.HttpFormats._
import db.data.Category.CategoryId
import play.api.mvc.Results._

case class ErrorResponse(msg: String, source: scala.Option[String])

case class ErrorListResponse(msg: String, sources: List[String])

object ErrorResponses {

  def loginFailed() = BadRequest(ErrorResponse(s"invalid email or password", source = None).toJson)

  def emailExists(email: String) =
    Conflict(ErrorResponse(msg = s"with email $email already exists", source = Some("email")).toJson)

  def googleIdExists(googleId: String) =
    Conflict(ErrorResponse(s"google id $googleId already exists", source = Some("googleId")).toJson)

  def invalidCategory(categoryId: CategoryId) =
    BadRequest(ErrorResponse(s"invalid category id $categoryId", source = Some("categoryId")).toJson)

  def fieldIsTooLong(field: String, length: Int = 255) =
    BadRequest(ErrorResponse(s"field $field is too long (max $length)", source = Some(field)).toJson)

  def badJson(json: String) = BadRequest(ErrorResponse(s"bad json", source = Some(json)).toJson)

  def invalidFieldsFormat(fields: List[String]) =
    BadRequest(ErrorListResponse(s"invalid fields format", sources = fields).toJson)
}
