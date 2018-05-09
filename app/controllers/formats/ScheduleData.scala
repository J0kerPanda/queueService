package controllers.formats


import db.data.CustomScheduleData
import org.joda.time.Period

case class ScheduleData(period: Period, schedules: List[CustomScheduleData])
