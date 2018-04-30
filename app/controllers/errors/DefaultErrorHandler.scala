package controllers.errors

import controllers.Responses
import javax.inject.Singleton
import play.api.http.HttpErrorHandler
import play.api.libs.json.JsResultException
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._

@Singleton
class DefaultErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(Status(statusCode)("A client error occurred: " + message))
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = exception match {

    case JsResultException((path, _) :: _) =>
      Future.successful(Responses.invalidField(path.toJsonString))

    case _ =>
      Future.successful(InternalServerError("A server error occurred: " + exception.getMessage))
  }
}