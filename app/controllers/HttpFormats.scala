package controllers

import db.data.{Appointment, AppointmentStatus, Category, User}
import play.api.libs.json._

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

  implicit val appointmentFormat: OFormat[Appointment] = Json.format[Appointment]

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
