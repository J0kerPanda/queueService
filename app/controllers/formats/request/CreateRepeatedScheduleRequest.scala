package controllers.formats.request

import db.data.AppointmentInterval
import org.joda.time.{LocalDate, Period}

case class CreateRepeatedScheduleRequest(repeatDate: LocalDate,
                                         repeatPeriod: Period,
                                         appointmentIntervals: List[AppointmentInterval],
                                         appointmentDuration: Period,
                                         place: String)
