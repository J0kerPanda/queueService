package controllers.formats

import db.data.Category.CategoryId

case class UserInputData(firstName: String,
                         surname: String,
                         patronymic: String,
                         email: String,
                         password: String,
                         googleId: String,
                         categoryId: CategoryId)