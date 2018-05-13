package controllers.util

import akka.actor.ActorSystem
import controllers.errors.ErrorResponses
import play.api.libs.json.{JsObject, Reads}
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.ExecutionContext

object ControllerUtils {

  def getExecutionContext(system: ActorSystem): ExecutionContext = system.dispatchers.lookup("api-dispatcher")

  def extractJsObject[T](extractor: T => Result)(implicit req: Request[AnyContent], r: Reads[T]): Result =
    req.body.asJson match {

      case Some(obj: JsObject) => extractor(obj.as[T])

      case json => ErrorResponses.badJson(json.toString)
  }
}
