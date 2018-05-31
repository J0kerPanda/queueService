package controllers.formats

import controllers.errors.{ErrorListResponse, ErrorResponse}
import controllers.formats.request.{LoginRequest, RegistrationRequest}
import controllers.formats.response.{GenericScheduleFormat, HostDataFormat, ScheduleListDataFormat}
import db.DatabaseFormats.IdEntity
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

  implicit def IntEntityWriteConverter[D](obj: IdEntity[Int, D])(implicit w: Writes[D]): JsValue = {
    JsObject(("id", JsNumber(obj.id)) +: w.writes(obj.data).as[JsObject].fields)
  }

  implicit def LongEntityWriteConverter[D](obj: IdEntity[Long, D])(implicit w: Writes[D]): JsValue = {
    JsObject(("id", JsNumber(obj.id)) +: w.writes(obj.data).as[JsObject].fields)
  }

  implicit val userDataWrite: Writes[UserData] = (o: UserData) => Json.writes[UserData].writes(o) - "password"

  implicit val userWrite: Writes[User] = (o: User) => IntEntityWriteConverter(o)

  implicit val appointmentDataWrite: Writes[AppointmentData] = Json.writes[AppointmentData]

  implicit val appointmentWrite: Writes[Appointment] = (o: Appointment) => LongEntityWriteConverter(o)

  implicit val errorResponseWrite: Writes[ErrorResponse] = Json.writes[ErrorResponse]

  implicit val errorListResponseWrite: Writes[ErrorListResponse] = Json.writes[ErrorListResponse]

  implicit val genericScheduleWrite: Writes[GenericScheduleFormat] = Json.writes[GenericScheduleFormat]

  implicit val scheduleListDataWrite: Writes[ScheduleListDataFormat] = Json.writes[ScheduleListDataFormat]

  implicit val hostDataWrite: Writes[HostDataFormat] = Json.writes[HostDataFormat]

  implicit val genericAppointmentWrite: Writes[GenericAppointment] = Json.writes[GenericAppointment]

  implicit class WriteConverter[T](obj: T)(implicit w: Writes[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class OptionWriteConverter[T](obj: Option[T])(implicit w: Writes[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class ListWriteConverter[T](objs: List[T])(implicit w: Writes[T]) {
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
      case JsString(value) => Try(JsSuccess(Period.parse(value))).toOption.getOrElse(JsError())

      case JsNumber(value) => Try(JsSuccess(new Period(value.toLong))).toOption.getOrElse(JsError())

      case _ => JsError()
    }
  }

  implicit val appointmentDataRead: Reads[AppointmentData] = Json.reads[AppointmentData]

  implicit val loginDataRead: Reads[LoginRequest] = Json.reads[LoginRequest]

  implicit val userInputDataRead: Reads[RegistrationRequest] = Json.reads[RegistrationRequest]

  implicit val scheduleDataRead: Reads[ScheduleData] = Json.reads[ScheduleData]

  implicit val repeatedScheduleDataRead: Reads[RepeatedScheduleData] = Json.reads[RepeatedScheduleData]
}
