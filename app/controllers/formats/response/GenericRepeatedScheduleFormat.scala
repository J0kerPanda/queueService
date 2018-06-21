package controllers.formats.response

import db.data.AppointmentInterval
import db.data.Schedule.ScheduleId
import org.joda.time.Period

case class GenericRepeatedScheduleFormat (id: ScheduleId,
                                          repeatPeriod: Period,
                                          appointmentIntervals: List[AppointmentInterval],
                                          appointmentDuration: Period,
                                          place: String)
