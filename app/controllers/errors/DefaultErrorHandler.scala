package controllers.errors

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
    if (message.nonEmpty) {
      Future.successful(Status(statusCode)(s"A client error occured: $message"))
    } else {
      Future.successful(Status(statusCode))
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = exception match {

    case JsResultException(errors) =>
      Future.successful(ErrorResponses.invalidFieldsFormat(
        errors.map(_._1.toJsonString.substring(jsonPathPrefix.length)).toList
      ))

    case _ =>
      exception.printStackTrace()
      Future.successful(InternalServerError("A server error occurred: " + exception.getMessage))
  }
}