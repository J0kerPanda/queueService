package modules

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.filters.AuthorizedRoutes
import controllers.auth.{AppAuthorizedRoutes, AppHandlerCache}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class DeadboltModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
//    bind[AuthorizedRoutes].to[AppAuthorizedRoutes],
    bind[HandlerCache].to[AppHandlerCache]
  )
}
