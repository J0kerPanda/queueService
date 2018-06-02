package controllers.formats.response

import db.data.AppointmentInterval
import db.data.Schedule.ScheduleId
import org.joda.time.{LocalDate, Period}

case class GenericScheduleFormat(id: ScheduleId,
                                 date: LocalDate,
                                 appointmentIntervals: List[AppointmentInterval],
                                 appointmentDuration: Period,
                                 place: String)