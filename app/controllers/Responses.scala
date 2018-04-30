package controllers

import db.data.Category.CategoryId
import play.api.libs.json.Writes

final case class OkResponse[T](entity: Option[T])(implicit w: Writes[T])

final case class ErrorResponse(msg: String, field: scala.Option[String])

object Responses {

  def emailExists(email: String) =
    ErrorResponse(msg = s"with email $email already exists", field = Some("email"))

  def googleIdExists(googleId: String) =
    ErrorResponse(s"google id $googleId already exists", field = Some("googleId"))

  def invalidCategory(categoryId: CategoryId) =
    ErrorResponse(s"invalid category id $categoryId", field = Some("categoryId"))

  def fieldIsTooLong(field: String, length: Int = 255) =
    ErrorResponse(s"field $field is too long (max $length)", field = Some(field))
}
