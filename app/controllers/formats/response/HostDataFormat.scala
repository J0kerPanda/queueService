package controllers.formats.response

import db.data.User.UserId

case class HostDataFormat(id: UserId, firstName: String, surname: String, patronymic: String)
