package controllers.auth

import be.objectify.deadbolt.scala.filters.{AuthorizedRoute, FilterConstraints, _}
import javax.inject.Inject


class AppAuthorizedRoutes @Inject() (filterConstraints: FilterConstraints) extends AuthorizedRoutes {

  override val routes: Seq[AuthorizedRoute] =
    Seq(
      AuthorizedRoute(Post, "/api/user/login", filterConstraints.subjectNotPresent),
      AuthorizedRoute(Post, "/api/user/register", filterConstraints.subjectNotPresent),
      AuthorizedRoute(Post, "/api/user/promote", filterConstraints.subjectPresent),
      AuthorizedRoute(Get, "/api/user/hosts", filterConstraints.subjectPresent),
      AuthorizedRoute(Get, "/api/user/$id<[^/]+>", filterConstraints.subjectPresent),
      AuthorizedRoute(Any, "/api/appointment/*", filterConstraints.subjectPresent),
      AuthorizedRoute(Any, "/api/schedule/*", filterConstraints.subjectPresent)
    )
}