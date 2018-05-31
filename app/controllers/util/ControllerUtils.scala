package controllers.util

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.AuthenticatedRequest
import controllers.errors.ErrorResponses
import play.api.libs.json.Reads
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

object ControllerUtils {

  def getExecutionContext(system: ActorSystem): ExecutionContext = system.dispatchers.lookup("api-dispatcher")

  def extractJsObject[T](extractor: T => Result)(implicit req: Request[AnyContent], r: Reads[T]): Result =
    req.body.asJson match {

      case Some(j) => extractor(j.as[T])

      case json => ErrorResponses.badJson(json.toString)
  }

  def extractJsObjectAuth[T](extractor: T => Future[Result])
                            (implicit
                             req: AuthenticatedRequest[AnyContent],
                             r: Reads[T],
                             ec: ExecutionContext): Future[Result] = req.body.asJson match {

      case Some(j) => extractor(j.as[T])

      case json => Future.successful(ErrorResponses.badJson(json.toString))
    }
}
