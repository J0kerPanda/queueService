package controllers

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

object ControllerUtils {

  def getExecutionContext(system: ActorSystem): ExecutionContext = system.dispatchers.lookup("api-dispatcher")
}
