package controllers

import db.data.User
import play.api.libs.json._

object HttpFormats {

  implicit val userJsonFormat: OFormat[User] = Json.format[User]

  implicit class Converter[T](obj: T)(implicit format: OFormat[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class Option[T](obj: scala.Option[T])(implicit format: OFormat[T]) {
    def toJson: JsValue = Json.toJson(obj)
  }

  implicit class ListConverter[T](objs: List[T])(implicit format: OFormat[T]) {
    def toJson: JsValue = Json.toJson(objs)
  }
}
