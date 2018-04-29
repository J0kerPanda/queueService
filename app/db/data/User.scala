package db.data

import cats.data.NonEmptyList
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie._

object User {

  def insert(u: User): ConnectionIO[Int] = {
    sql"""INSERT INTO "User" ("id", firstName, surname, lastName, email, "password", googleId, categoryId, isSuperUser, isBlocked) VALUES
         |(${u.id}, ${u.firstName}, ${u.surname}, ${u.lastName}, ${u.email},
         |${u.password}, ${u.googleId}, ${u.categoryId}, ${u.isSuperUser}, ${u.isBlocked})"""
      .stripMargin
      .update()
      .run
  }

  def selectById(id: Int): ConnectionIO[Option[User]] = {
    sql"""SELECT "id", firstName, surname, lastName, email, "password", googleId, categoryId, isSuperUser, isBlocked FROM "User" WHERE id = $id"""
      .query[User]
      .option
  }

  def selectByIds(ids: NonEmptyList[Int], isBlocked: Boolean = false): ConnectionIO[List[User]] = {
    (fr"""SELECT "id", firstName, surname, lastName, email, "password", googleId, categoryId, isSuperUser, isBlocked FROM "User" WHERE isblocked = $isBlocked AND""" ++ Fragments.in(fr"id", ids))
      .stripMargin
      .query[User]
      .to[List]
  }
}

case class User(id: Int,
                firstName: String,
                surname: String,
                lastName: String,
                email: String,
                password: String,
                googleId: String,
                categoryId: Int,
                isSuperUser: Boolean = false,
                isBlocked: Boolean = false)
