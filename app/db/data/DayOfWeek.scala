package db.data

import enumeratum._

import scala.collection.immutable

sealed abstract class DayOfWeek(val dbName: String) extends EnumEntry

object DayOfWeek extends Enum[DayOfWeek] {

  val values: immutable.IndexedSeq[DayOfWeek] = findValues

  case object Monday extends DayOfWeek("mon")
  case object Tuesday extends DayOfWeek("tue")
  case object Wednesday extends DayOfWeek("wed")
  case object Thursday extends DayOfWeek("thu")
  case object Friday extends DayOfWeek("fri")
  case object Saturday extends DayOfWeek("sat")
  case object Sunday extends DayOfWeek("sun")
}

