package controllers.formats.response

import org.joda.time.{LocalDate, LocalTime, Period}

case class GenericScheduleFormat(date: LocalDate,
                                 start: LocalTime,
                                 end: LocalTime,
                                 appointmentDuration: Period,
                                 place: String)