package controllers

import db.data.{Appointment, AppointmentStatus, Category, User}
import org.joda.time.DateTime
import play.api.libs.json._

import scala.util.Try

object HttpFormats {

  implicit val userJsonFormat: OFormat[User] = Json.format[User]
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

  implicit def okResponseFormat[T]: OFormat[OkResponse[T]] = Json.format[OkResponse[T]]

  implicit val errorResponseFormat: OFormat[ErrorResponse] = Json.format[ErrorResponse]

  implicit class Converter[T](obj: T)(implicit format: OFormat[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class Option[T](obj: scala.Option[T])(implicit format: OFormat[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class ListConverter[T](objs: List[T])(implicit format: OFormat[T]) {
    def toJson: JsValue = Json.toJson(objs)
  }
}
