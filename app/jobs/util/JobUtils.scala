package jobs.util

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

object JobUtils {

  def getExecutionContext(system: ActorSystem): ExecutionContext = system.dispatchers.lookup("job-dispatcher")
}
