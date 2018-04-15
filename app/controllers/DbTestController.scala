package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class ApiController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def test = Action {
    Ok("ok!")
  }
}