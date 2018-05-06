package controllers.formats

import org.joda.time.LocalDate

case class ScheduleDates(period: Long, default: List[LocalDate], custom: List[LocalDate])
