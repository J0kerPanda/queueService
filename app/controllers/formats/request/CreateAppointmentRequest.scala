package controllers.formats.request

import db.data.Schedule.ScheduleId
import db.data.User.UserId
import org.joda.time.{LocalDate, LocalTime}

case class CreateAppointmentRequest(hostId: UserId,
                                    visitorId: UserId,
                                    scheduleId: ScheduleId,
                                    date: LocalDate,
                                    time: LocalTime)
