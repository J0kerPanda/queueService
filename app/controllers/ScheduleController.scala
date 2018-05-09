package controllers

import akka.actor.ActorSystem
import controllers.errors.ErrorResponses
import controllers.formats.HttpFormats._
import controllers.formats.ScheduleDatesData
import controllers.util.ControllerUtils
import controllers.util.ControllerUtils._
import db.ConnectionUtils
import db.data.User.UserId
import db.data.{CustomScheduleData, DefaultScheduleData, HostMeta, Schedule}
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.Logger
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class ScheduleController @Inject()(cu: ConnectionUtils, cc: ControllerComponents, system: ActorSystem)
  extends AbstractController(cc) {

  private implicit val ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  //todo unique constraint errors
  def createDefault = Action { request =>
    extractJsObject[DefaultScheduleData](request) { sd =>

      Schedule
        .insertDefault(sd)
        .transact(cu.transactor)
        .attempt
        .unsafeRunSync() match {

        case Left(err) =>
          Logger.error("schedule error", err)
          ErrorResponses.invalidScheduleData

        case Right(id) => Created(id.toString)
      }
    }
  }

  def createCustom = Action { request =>
    extractJsObject[CustomScheduleData](request) { sd =>

      Schedule
        .insertCustom(sd)
        .transact(cu.transactor)
        .attempt
        .unsafeRunSync() match {

        case Left(err) =>
          Logger.error("schedule error", err)
          ErrorResponses.invalidScheduleData

        case Right(id) => Created(id.toString)
      }
    }
  }

  def getDates(hostId: UserId): Action[AnyContent] = Action {

    val intervalOpt = HostMeta.selectById(hostId).transact(cu.transactor).unsafeRunSync().map(_.appointmentPeriod)

    intervalOpt.map { interval =>
      val from = new LocalDate()
      val to = from.plus(interval.toStandardDays)
      val (default, custom) = Schedule.selectSchedules(hostId, from, to)
        .transact(cu.transactor)
        .unsafeRunSync()

//      val defaultDays = default.map(_.day.number).toSet
//      val customDates = custom.map(_.date).toSet
//      val defaultDates = getDefaultDates(customDates, defaultDays, from, to)

      Ok("")
    }
      .getOrElse(ErrorResponses.invalidHostUser(hostId))
  }

  private def getDefaultDates(customDates: Set[LocalDate],
                              defaultDayNumbers: Set[Int],
                              current: LocalDate,
                              end: LocalDate,
                              acc: List[LocalDate] = Nil): List[LocalDate] = {

    if (current.isBefore(end)) {
      val defaultDatePossible = !customDates.contains(current) && defaultDayNumbers.contains(current.getDayOfWeek)
      val newAcc = if (defaultDatePossible) current :: acc else acc
      getDefaultDates(customDates, defaultDayNumbers, current.plusDays(1), end, newAcc)
    } else {
      acc
    }
  }

  //todo add default, remove default -> composition
}