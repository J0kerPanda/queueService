package controllers.formats.response

import db.data.User.UserId
import org.joda.time.Period

case class ScheduleListDataFormat(hostId: UserId, period: Period, schedules: List[GenericScheduleFormat])
