package controllers.formats

import db.data.HostMeta.UserId
import org.joda.time.{LocalDate, LocalTime}

case class CustomScheduleData(hostId: UserId, date: LocalDate, start: LocalTime, end: LocalTime)
