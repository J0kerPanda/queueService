package controllers.formats.request

import db.data.Schedule.ScheduleId
import db.data.User.UserId
import org.joda.time.LocalTime

case class CreateAppointmentRequest(scheduleId: ScheduleId,
                                    visitorId: UserId,
                                    start: LocalTime,
                                    end: LocalTime)
