package controllers.formats

import db.data.Category.CategoryId

case class UserInputData(firstName: String,
                         surname: String,
                         lastName: String,
                         email: String,
                         password: String,
                         googleId: String,
                         categoryId: CategoryId)