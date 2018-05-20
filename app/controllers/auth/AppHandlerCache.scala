package controllers.auth

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{DeadboltHandler, HandlerKey}
import javax.inject.Singleton

object AppHandlerCache {

  val defaultHandlerName: String = "default_handler"
}

@Singleton
class AppHandlerCache extends HandlerCache {

  private val defaultHandler = new AppDeadboltHandler()

  // HandlerKeys is an user-defined object, containing instances of a case class that extends HandlerKey
  private val handlers: Map[Any, DeadboltHandler] = Map(defaultHandlerName -> defaultHandler)

  // Get the default handler.
  override def apply(): DeadboltHandler = defaultHandler

  // Get a named handler
  override def apply(handlerKey: HandlerKey): DeadboltHandler = handlers(handlerKey)
}