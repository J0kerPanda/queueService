package jobs

import akka.actor.ActorSystem
import db.DbConnectionUtils
import db.data.RepeatedSchedule
import javax.inject.{Inject, Singleton}
import jobs.util.JobUtils
import doobie.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class ScheduleGenerator @Inject()(system: ActorSystem, cu: DbConnectionUtils) {
  private implicit val ec: ExecutionContext = JobUtils.getExecutionContext(system)
  system.scheduler.schedule(0.seconds, 1.days, () => RepeatedSchedule.generateSchedules().transact(cu.transactor).unsafeRunSync())
}
