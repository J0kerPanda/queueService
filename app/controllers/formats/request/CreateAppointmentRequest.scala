package controllers.formats.request

import db.data.Schedule.ScheduleId
import org.joda.time.LocalTime

case class CreateAppointmentRequest(scheduleId: ScheduleId,
                                    start: LocalTime,
                                    end: LocalTime)
