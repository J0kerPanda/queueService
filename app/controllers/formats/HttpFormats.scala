package controllers.formats

import controllers.errors.{ErrorListResponse, ErrorResponse}
import db.data.{Appointment, AppointmentStatus, Category, User}
import org.joda.time.DateTime
import play.api.libs.json._

import scala.util.Try

object HttpFormats {

  implicit val userJsonFormat: OFormat[User] = new OFormat[User] {

    override def writes(o: User): JsObject = Json.writes[User].writes(o) - "password"

    override def reads(json: JsValue): JsResult[User] = Json.reads[User].reads(json)
  }

  implicit val categoryJsonFormat: OFormat[Category] = Json.format[Category]

  implicit object appointmentStatusReadFormat extends Reads[AppointmentStatus] {
    override def reads(json: JsValue): JsResult[AppointmentStatus] = json match {
      case JsString(value) if AppointmentStatus.lowerCaseNamesToValuesMap.contains(value.toLowerCase) =>
        JsSuccess(AppointmentStatus.lowerCaseNamesToValuesMap(value.toLowerCase))

      case _ => JsError()
    }
  }

  implicit object appointmentStatusWriteFormat extends Writes[AppointmentStatus] {
    override def writes(status: AppointmentStatus): JsValue = JsString(status.dbName)
  }

  implicit object dateTimeReadFormat extends Reads[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(value) => Try(JsSuccess(DateTime.parse(value))).toOption.getOrElse(JsError())

      case _ => JsError()
    }
  }

  implicit object dateTimeWriteFormat extends Writes[DateTime] {
    override def writes(dt: DateTime): JsValue = JsString(dt.toString())
  }

  implicit val appointmentFormat: OFormat[Appointment] = Json.format[Appointment]

  implicit val errorResponseWriteFormat: Writes[ErrorResponse] = Json.writes[ErrorResponse]

  implicit val errorListResponseWriteFormat: Writes[ErrorListResponse] = Json.writes[ErrorListResponse]

  implicit val userInputDataReadFormat: Reads[UserInputData] = Json.reads[UserInputData]

  implicit val loginDataReadFormat: Reads[LoginData] = Json.reads[LoginData]

  implicit class Converter[T](obj: T)(implicit w: Writes[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class OptionConverter[T](obj: scala.Option[T])(implicit w: Writes[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class ListConverter[T](objs: List[T])(implicit w: Writes[T]) {
    def toJson: JsValue = Json.toJson(objs)
  }
}
