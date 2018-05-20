package controllers.errors

import controllers.formats.HttpFormats._
import db.data.User.UserId
import play.api.mvc.Results._

case class ErrorResponse(msg: String, source: Option[String])

case class ErrorListResponse(msg: String, sources: List[String])

object ErrorResponses {

  val loginFailed = BadRequest(ErrorResponse("invalid email or password", source = None).toJson)

  def invalidHostUser(hostId: UserId) =
    BadRequest(ErrorResponse(s"invalid hostId $hostId", source = Some("hostId")).toJson)

  def invalidVisitorUser(visitorId: UserId) =
    BadRequest(ErrorResponse(s"invalid visitorId $visitorId", source = Some("visitorId")).toJson)

  def emailExists(email: String) =
    Conflict(ErrorResponse(msg = s"with email $email already exists", source = Some("email")).toJson)

  val invalidScheduleData = BadRequest(ErrorResponse("invalid schedule data", source = None).toJson)

  def fieldIsTooLong(field: String, length: Int = 255) =
    BadRequest(ErrorResponse(s"field $field is too long (max $length)", source = Some(field)).toJson)

  def badJson(json: String) = BadRequest(ErrorResponse(s"bad json", source = Some(json)).toJson)

  def invalidFieldsFormat(fields: List[String]) =
    BadRequest(ErrorListResponse(s"invalid fields format", sources = fields).toJson)
}
