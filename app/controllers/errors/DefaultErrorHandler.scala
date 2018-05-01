package controllers.errors

import controllers.util.Responses
import javax.inject.Singleton
import play.api.http.HttpErrorHandler
import play.api.libs.json.JsResultException
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._

@Singleton
class DefaultErrorHandler extends HttpErrorHandler {

  private val jsonPathPrefix = "obj."

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(Status(statusCode)("A client error occurred: " + message))
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = exception match {

    case JsResultException(errors) =>
      Future.successful(Responses.invalidFieldsFormat(
        errors.map(_._1.toJsonString.substring(jsonPathPrefix.length)).toList
      ))

    case _ =>
      Future.successful(InternalServerError("A server error occurred: " + exception.getMessage))
  }
}