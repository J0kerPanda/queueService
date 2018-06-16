package controllers.auth

import be.objectify.deadbolt.scala.models.Role

object Roles {

  case object Host extends Role {
    override def name: String = "host"
  }
}
