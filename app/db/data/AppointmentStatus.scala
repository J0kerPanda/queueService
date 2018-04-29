package db.data

import enumeratum.{Enum, EnumEntry}
import doobie.postgres.implicits._
import doobie.util.meta.Meta

import scala.collection.immutable

sealed abstract class AppointmentStatus(val dbName: String) extends EnumEntry

object AppointmentStatus extends Enum[AppointmentStatus] {

  val values: immutable.IndexedSeq[AppointmentStatus] = findValues

  case object Pending extends AppointmentStatus("pending")
  case object Finished extends AppointmentStatus("finished")
  case object CancelledByUser extends AppointmentStatus("cancelledByUser")
  case object CancelledByHost extends AppointmentStatus("cancelledByHost")

  implicit val AppointmentStatusMeta: Meta[AppointmentStatus] = pgEnumStringOpt(
    "visit_status",
    name => AppointmentStatus.namesToValuesMap.get(name),
    enum => enum.dbName
  )
}
