package controllers.auth

import be.objectify.deadbolt.scala.models.{Permission, Role, Subject}
import db.data.User.UserId

case class AuthUser(id: UserId, roles: List[Role], permissions: List[Permission]) extends Subject {
  override def identifier: String = id.toString
}
