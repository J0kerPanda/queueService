package db.data

import enumeratum._
import org.joda.time.DateTime

import scala.collection.immutable
import org.joda.time.DateTimeConstants._

sealed abstract class DayOfWeek(val dbName: String, val number: Int) extends EnumEntry

object DayOfWeek extends Enum[DayOfWeek] {

  val values: immutable.IndexedSeq[DayOfWeek] = findValues

  case object Monday extends DayOfWeek("mon", MONDAY)
  case object Tuesday extends DayOfWeek("tue", TUESDAY)
  case object Wednesday extends DayOfWeek("wed", WEDNESDAY)
  case object Thursday extends DayOfWeek("thu", THURSDAY)
  case object Friday extends DayOfWeek("fri", FRIDAY)
  case object Saturday extends DayOfWeek("sat", SATURDAY)
  case object Sunday extends DayOfWeek("sun", SUNDAY)

  private def fromDayNumber(i: Int): DayOfWeek = values.map(v => v.number -> v).toMap.apply(i)

  def fromDate(dateTime: DateTime): DayOfWeek = fromDayNumber(dateTime.dayOfWeek().get())
}

