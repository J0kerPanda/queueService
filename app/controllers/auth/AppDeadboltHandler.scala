package controllers.auth

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.models.{Role, Subject}
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import controllers.auth.Roles.Host
import controllers.util.{AuthUtils, ControllerUtils}
import db.DbConnectionUtils
import db.data.{User, UserData}
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Request, Result, _}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AppDeadboltHandler @Inject()(cu: DbConnectionUtils, system: ActorSystem) extends DeadboltHandler {

  private implicit val ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  override def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future.successful(None)

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = Future(
    request.subject.orElse {
      request.session.get(AuthUtils.UserIdKey) match {
        case Some(id) =>
          //todo cache?
          User.selectById(id.toInt)
          .transact(cu.transactor)
          .unsafeRunSync()
          .collect { case u if !u.data.isBlocked => AuthUser(u.id, extractRoles(u.data), Nil) }

        case _ =>
          None
      }
    }
  )

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = Future.successful(Results.Forbidden)

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] =
    Future.successful(None)

  private def extractRoles(userData: UserData): List[Role] = {
    Option(Host).filter(_ => userData.isHost).toList
  }
}
