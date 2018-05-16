package controllers.formats.request

import db.data.AppointmentData
import db.data.User.UserId
import org.joda.time.{LocalDate, LocalTime}

case class CreateAppointmentRequest(hostId: UserId,
                                    visitorId: UserId,
                                    date: LocalDate,
                                    start: LocalTime,
                                    end: LocalTime) {

  def toAppointmentData() = AppointmentData(
    hostId,
    visitorId,
    date,
    start,
    end
  )
}
