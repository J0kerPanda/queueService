package controllers.formats

import db.data.DayOfWeek
import db.data.HostMeta.UserId
import org.joda.time.LocalTime

case class DefaultScheduleData(hostId: UserId, day: DayOfWeek, start: LocalTime, end: LocalTime)