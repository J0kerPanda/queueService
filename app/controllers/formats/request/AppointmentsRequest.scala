package controllers.formats.request

import db.data.Schedule.ScheduleId
import db.data.User.UserId
import org.joda.time.LocalDate

case class AppointmentsRequest(hostId: UserId,
                               date: LocalDate,
                               scheduleIds: List[ScheduleId],
                               isCustom: Boolean)
