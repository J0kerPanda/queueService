package controllers.auth

import be.objectify.deadbolt.scala.models.Subject
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import play.api.mvc.{Request, Result, _}

import scala.concurrent.Future

class AppDeadboltHandler extends DeadboltHandler {

  override def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future.successful(None)

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = Future.successful(request.subject) //todo

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = Future.successful(Results.Forbidden)

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] =
    Future.successful(None)
}
