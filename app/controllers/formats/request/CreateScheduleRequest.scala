package controllers.formats.request

import db.data.AppointmentInterval
import org.joda.time.{LocalDate, Period}

case class CreateScheduleRequest(date: LocalDate,
                                 appointmentIntervals: List[AppointmentInterval],
                                 appointmentDuration: Period,
                                 place: String)
