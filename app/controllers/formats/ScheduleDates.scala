package controllers.formats

import org.joda.time.LocalDate

case class ScheduleDates(default: List[LocalDate], custom: List[LocalDate])
