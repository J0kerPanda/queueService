package db.data

import java.util.Date

import db.data.Appointment.Status.Pending
import enumeratum._

object Appointment {

  sealed abstract class Status(dbName: String) extends EnumEntry

  object Status extends Enum[Status] {

    val values = findValues

    case object Pending extends Status("pending")
    case object Finished extends Status("finished")
    case object CancelledByUser extends Status("cancelledByUser")
    case object CancelledByHost extends Status("cancelledByHost")
  }
}

case class Appointment(id: Long,
                       hostId: Long,
                       visitorId: Long,
                       date: Date,
                       status: Appointment.Status = Pending)
