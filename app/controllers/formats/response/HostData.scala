package controllers.formats.response

import db.data.User.UserId

case class HostData(id: UserId, firstName: String, surname: String, patronymic: String)
