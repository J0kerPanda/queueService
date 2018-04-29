package db.data

import java.util.Date

import db.data.AppointmentStatus._

case class Appointment(id: Long,
                       hostId: Int,
                       visitorId: Int,
                       date: Date,
                       status: AppointmentStatus = Pending)
