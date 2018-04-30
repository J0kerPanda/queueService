package controllers

import db.data.Category.CategoryId
import play.api.libs.json.Writes
import play.api.mvc.Results._

final case class OkResponse[T](entity: Option[T])(implicit f: Writes[T])

final case class ErrorResponse(msg: String, field: Option[String])

object Responses {

  def emailExists(email: String) = Conflict(ErrorResponse(s"with email $email already exists", field = Some("email")))

  def googleIdExists(googleId: String) = Conflict(ErrorResponse(s"google id $googleId already exists", field = Some("googleId")))

  def invalidCategory(categoryId: CategoryId) = BadRequest(ErrorResponse(s"invalid category id $categoryId", field = Some("categoryId")))

  def fieldIsTooLong(field: String) = BadRequest(ErrorResponse(s"field $field is too long", field = Some(field)))
}
