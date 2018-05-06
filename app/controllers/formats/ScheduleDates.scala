package controllers.formats

import org.joda.time.LocalDate

case class ScheduleDates(interval: Long, default: List[LocalDate], custom: List[LocalDate])
