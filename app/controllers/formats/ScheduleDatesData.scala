package controllers.formats

import org.joda.time.LocalDate

case class ScheduleDatesData(period: Long, default: List[LocalDate], custom: List[LocalDate])
