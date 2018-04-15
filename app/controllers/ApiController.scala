package controllers

import javax.inject.{Inject, Singleton}

import db.ConnectionUtils
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class ApiController @Inject()(cu: ConnectionUtils, cc: ControllerComponents, ec: ExecutionContextProvider) extends AbstractController(cc) {

  private implicit val _ec: ExecutionContext = ec.get()

  def test: Action[AnyContent] = Action.async {
    cu.insert().unsafeToFuture().map(c => Ok(c.toString))
  }
}