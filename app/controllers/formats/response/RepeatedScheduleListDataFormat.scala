package controllers.formats.response

import db.data.User.UserId
import org.joda.time.LocalDate

case class RepeatedScheduleListDataFormat(hostId: UserId, schedules: Map[LocalDate, GenericRepeatedScheduleFormat])