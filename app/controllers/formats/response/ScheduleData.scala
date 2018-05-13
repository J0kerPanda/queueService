package controllers.formats.response

import db.data.GenericSchedule
import db.data.User.UserId
import org.joda.time.Period

case class ScheduleData(hostId: UserId, period: Period, schedules: List[GenericSchedule])
