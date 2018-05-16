package controllers.formats

import controllers.errors.{ErrorListResponse, ErrorResponse}
import controllers.formats.request.{AppointmentsRequest, CreateAppointmentRequest, LoginData, UserInputData}
import controllers.formats.response.{HostData, ScheduleData}
import db.data._
import org.joda.time.{DateTime, LocalDate, LocalTime, Period}
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

  implicit object periodWrite extends Writes[Period] {
    override def writes(p: Period): JsValue = JsString(p.toString())
  }

  implicit val userDataWrite: Writes[UserData] = (o: UserData) => Json.writes[UserData].writes(o) - "password"

  implicit val userWrite: Writes[User] = (o: User) => Json.writes[User].writes(o)

  implicit val categoryDaaWrite: Writes[CategoryData] = Json.writes[CategoryData]

  implicit val categoryWrite: Writes[Category] = Json.writes[Category]

  implicit object appointmentStatusWrite extends Writes[AppointmentStatus] {
    override def writes(status: AppointmentStatus): JsValue = JsString(status.dbName)
  }

  implicit val appointmentDataWrite: Writes[AppointmentData] = Json.writes[AppointmentData]

  implicit val appointmentWrite: Writes[Appointment] = Json.writes[Appointment]

  implicit val errorResponseWrite: Writes[ErrorResponse] = Json.writes[ErrorResponse]

  implicit val errorListResponseWrite: Writes[ErrorListResponse] = Json.writes[ErrorListResponse]

  implicit val genericScheduleWrite: Writes[GenericSchedule] = Json.writes[GenericSchedule]

  implicit val scheduleDatesWrite: Writes[ScheduleData] = Json.writes[ScheduleData]

  implicit val hostDataWrite: Writes[HostData] = Json.writes[HostData]

  implicit val genericAppointmentWrite: Writes[GenericAppointment] = Json.writes[GenericAppointment]

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

  implicit object localTimeRead extends Reads[LocalTime] {
    override def reads(json: JsValue): JsResult[LocalTime] = json match {
      case JsString(value) =>
        Try(JsSuccess(LocalTime.parse(value))).toOption.getOrElse(JsError())

      case _ => JsError()
    }
  }

  implicit object localDateRead extends Reads[LocalDate] {
    override def reads(json: JsValue): JsResult[LocalDate] = json match {
      case JsString(value) => Try(JsSuccess(LocalDate.parse(value))).toOption.getOrElse(JsError())

      case _ => JsError()
    }
  }

  implicit object dateTimeRead extends Reads[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(value) => Try(JsSuccess(DateTime.parse(value))).toOption.getOrElse(JsError())

      case _ => JsError()
    }
  }

  implicit object periodRead extends Reads[Period] {
    override def reads(json: JsValue): JsResult[Period] = json match {
      case JsNumber(value) => Try(JsSuccess(new Period(value.toLong))).toOption.getOrElse(JsError())

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

  implicit val createAppointmentRequestRead: Reads[CreateAppointmentRequest] = Json.reads[CreateAppointmentRequest]

  implicit val appointmentDataRead: Reads[AppointmentData] = Json.reads[AppointmentData]

  implicit val loginDataRead: Reads[LoginData] = Json.reads[LoginData]

  implicit val userInputDataRead: Reads[UserInputData] = Json.reads[UserInputData]

  implicit val defaultScheduleDataRead: Reads[DefaultScheduleData] = Json.reads[DefaultScheduleData]

  implicit val customScheduleDataRead: Reads[CustomScheduleData] = Json.reads[CustomScheduleData]

  implicit val appointmentsRequest: Reads[AppointmentsRequest] = Json.reads[AppointmentsRequest]
}
