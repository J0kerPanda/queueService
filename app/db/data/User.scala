package db.data

import cats.data.NonEmptyList
import db.data.Category.CategoryId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._

object User {

  type UserId = Int

  def login(email: String, password: String): ConnectionIO[Option[User]] = {
    sql"""SELECT id, firstName, surname, lastName, email, password, googleId, categoryId, isHost, isBlocked FROM "User" WHERE email = $email AND password = crypt($password, password)"""
      .query[User]
      .option
  }

  def insert(u: User): ConnectionIO[UserId] = {
    sql"""INSERT INTO "User" (firstName, surname, lastName, email, password, googleId, categoryId, isHost, isBlocked) VALUES (${u.firstName}, ${u.surname}, ${u.lastName}, ${u.email}, crypt(${u.password}, gen_salt('bf', 8)), ${u.googleId}, ${u.categoryId}, ${u.isHost}, ${u.isBlocked})"""
      .update()
      .withUniqueGeneratedKeys[UserId]("id")
  }

  def selectById(id: UserId): ConnectionIO[Option[User]] = {
    sql"""SELECT id, firstName, surname, lastName, email, password, googleId, categoryId, isHost, isBlocked FROM "User" WHERE id = $id"""
      .query[User]
      .option
  }

  def selectByIds(ids: NonEmptyList[UserId], isBlocked: Boolean = false): ConnectionIO[List[User]] = {
    (fr"""SELECT id, firstName, surname, lastName, email, password, googleId, categoryId, isHost, isBlocked FROM "User" WHERE isblocked = $isBlocked AND""" ++ Fragments.in(fr"id", ids))
      .stripMargin
      .query[User]
      .to[List]
  }

  def forInsertion(firstName: String,
                   surname: String,
                   lastName: String,
                   email: String,
                   password: String,
                   googleId: String,
                   categoryId: CategoryId,
                   isHost: Boolean = false,
                   isBlocked: Boolean = false): User = {

    User(
      id = -1,
      firstName = firstName,
      surname = surname,
      lastName = lastName,
      email = email,
      password = password,
      googleId = googleId,
      categoryId = categoryId,
      isHost = isHost,
      isBlocked = isBlocked
    )
  }
}

case class User(id: UserId,
                firstName: String,
                surname: String,
                lastName: String,
                email: String,
                password: String,
                googleId: String,
                categoryId: CategoryId,
                isHost: Boolean,
                isBlocked: Boolean)
