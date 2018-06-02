package controllers.formats.response

import db.data.User.UserId
import org.joda.time.{LocalDate, Period}

case class ScheduleListDataFormat(hostId: UserId, period: Period, schedules: Map[LocalDate, GenericScheduleFormat])
