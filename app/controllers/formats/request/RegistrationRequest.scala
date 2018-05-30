package controllers.formats.request

case class RegistrationRequest(firstName: String,
                               surname: String,
                               patronymic: String,
                               email: String,
                               password: String)