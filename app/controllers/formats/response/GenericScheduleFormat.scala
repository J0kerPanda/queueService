package controllers.formats.response

import db.data.Schedule.ScheduleId
import org.joda.time.{LocalDate, LocalTime, Period}

case class GenericScheduleFormat(id: ScheduleId,
                                 date: LocalDate,
                                 start: LocalTime,
                                 end: LocalTime,
                                 appointmentDuration: Period,
                                 place: String)