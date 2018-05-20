package db.data

import cats.data.NonEmptyList
import db.DatabaseFormats.IdEntity
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._

object User {

  type UserId = Int

  private val selectUserSql = sql"""SELECT id, firstName, surname, patronymic, email, password, isHost, isBlocked FROM "User""""

  def login(email: String, password: String): ConnectionIO[Option[User]] = {
    (selectUserSql ++ Fragments.whereAnd(fr"email = $email", fr"password = crypt($password, password)"))
      .query[User]
      .option
  }

  def promote(id: UserId): ConnectionIO[UserId] = {
    sql"""UPDATE "User" SET ishost = TRUE WHERE id = $id"""
      .update
      .withUniqueGeneratedKeys("id")
  }

  def insert(u: UserData): ConnectionIO[UserId] = {
    sql"""INSERT INTO "User" (firstName, surname, patronymic, email, password, isHost, isBlocked) VALUES (${u.firstName}, ${u.surname}, ${u.patronymic}, ${u.email}, crypt(${u.password}, gen_salt('bf', 8)), ${u.isHost}, ${u.isBlocked})"""
      .update()
      .withUniqueGeneratedKeys("id")
  }

  def selectHosts(): ConnectionIO[List[User]] = {
    (selectUserSql ++ fr"WHERE ishost = true")
      .query[User]
      .to[List]
  }

  def selectById(id: UserId): ConnectionIO[Option[User]] = {
    (selectUserSql ++ fr"WHERE id = $id")
      .query[User]
      .option
  }

  def selectByIds(ids: NonEmptyList[UserId], isBlocked: Boolean = false): ConnectionIO[List[User]] = {
    (selectUserSql ++ Fragments.whereAnd(fr"isblocked = $isBlocked", Fragments.in(fr"id", ids)))
      .query[User]
      .to[List]
  }
}

case class UserData(firstName: String,
                    surname: String,
                    patronymic: String,
                    email: String,
                    password: String,
                    isHost: Boolean = false,
                    isBlocked: Boolean = false)

case class User(id: UserId, data: UserData) extends IdEntity[UserId, UserData]
