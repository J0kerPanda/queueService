package controllers.formats

import controllers.errors.{ErrorListResponse, ErrorResponse}
import db.data._
import org.joda.time.{DateTime, LocalDate, LocalTime}
import play.api.libs.json._

import scala.util.Try

object HttpFormats {

  // Write formats

  implicit object localTimeWrite extends Writes[LocalTime] {
    override def writes(time: LocalTime): JsValue = JsString(time.toString())
  }

  implicit object localDateWrite extends Writes[LocalDate] {
    override def writes(date: LocalDate): JsValue = JsString(date.toString())
  }

  implicit object dateTimeWrite extends Writes[DateTime] {
    override def writes(dt: DateTime): JsValue = JsString(dt.toString())
  }

  implicit val userWrite: Writes[User] = (o: User) => Json.writes[User].writes(o) - "password"

  implicit val categoryWrite: Writes[Category] = Json.writes[Category]

  implicit object appointmentStatusWrite extends Writes[AppointmentStatus] {
    override def writes(status: AppointmentStatus): JsValue = JsString(status.dbName)
  }



  implicit val appointmentWrite: Writes[Appointment] = Json.writes[Appointment]

  implicit val errorResponseWrite: Writes[ErrorResponse] = Json.writes[ErrorResponse]

  implicit val errorListResponseWrite: Writes[ErrorListResponse] = Json.writes[ErrorListResponse]

  implicit object dayOfWeekStatusWrite extends Writes[DayOfWeek] {
    override def writes(day: DayOfWeek): JsValue = JsString(day.dbName)
  }

  implicit val defaultScheduleWrite: Writes[DefaultSchedule] = Json.writes[DefaultSchedule]

  implicit val customScheduleWrite: Writes[CustomSchedule] = Json.writes[CustomSchedule]

  implicit class Converter[T](obj: T)(implicit w: Writes[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class OptionConverter[T](obj: Option[T])(implicit w: Writes[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class ListConverter[T](objs: List[T])(implicit w: Writes[T]) {
    def toJson: JsValue = Json.toJson(objs)
  }

  // Read formats

  implicit val loginDataRead: Reads[LoginData] = Json.reads[LoginData]

  implicit val userInputDataRead: Reads[UserInputData] = Json.reads[UserInputData]

  implicit object dateTimeRead extends Reads[DateTime] {

    override def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(value) => Try(JsSuccess(DateTime.parse(value))).toOption.getOrElse(JsError())

      case _ => JsError()
    }
  }

  implicit object appointmentStatusRead extends Reads[AppointmentStatus] {

    override def reads(json: JsValue): JsResult[AppointmentStatus] = json match {
      case JsString(value) if AppointmentStatus.lowerCaseNamesToValuesMap.contains(value.toLowerCase) =>
        JsSuccess(AppointmentStatus.lowerCaseNamesToValuesMap(value.toLowerCase))

      case _ => JsError()
    }
  }
}
