package db.data

import enumeratum.{Enum, EnumEntry}
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.util.meta.Meta

sealed abstract class AppointmentStatus(val dbName: String) extends EnumEntry

object AppointmentStatus extends Enum[AppointmentStatus] {

  val values = findValues

  case object Pending extends AppointmentStatus("pending")
  case object Finished extends AppointmentStatus("finished")
  case object CancelledByUser extends AppointmentStatus("cancelledByUser")
  case object CancelledByHost extends AppointmentStatus("cancelledByHost")

  implicit val AppointmentStatusMeta: Meta[AppointmentStatus] = pgEnumStringOpt(
    "visit_status",
    name => AppointmentStatus.lowerCaseNamesToValuesMap.get(name.toLowerCase),
    enum => enum.dbName
  )
}
