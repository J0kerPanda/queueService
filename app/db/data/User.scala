package db.data

import cats.data.NonEmptyList
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie._

object User {

  private type tupleType = User.type

  def insert(u: User): ConnectionIO[Int] = {
    sql"""INSERT INTO "User"
         |(id, firstname, surname, lastname, email, password, googleid, issuperuser, isblocked, categoryid)
         |VALUES (${u.id}, ${u.firstName}, ${u.surname}, ${u.lastName}, ${u.email},
         |${u.password}, ${u.googleId}, ${u.isSuperUser}, ${u.isBlocked}, ${u.categoryId})"""
      .stripMargin
      .update()
      .run
  }

  def selectLast: ConnectionIO[User] = {
    sql"""SELECT (id, firstName, surname, lastName, email, password, googleId, categoryId, isSuperUser, isBlocked)
         |FROM "User"
         |WHERE id = (SELECT lastval())""".stripMargin
      .query[User]
      .unique
  }

  //TODO SELECT USER?????????

  def selectById(id: Long): ConnectionIO[User] = {
    sql"""SELECT ("id", firstName, surname, lastName, email, password, googleId, categoryId, isSuperUser, isBlocked)
         |FROM "User"
         |WHERE id = $id"""
      .stripMargin
      .query[User]
      .unique
  }

  def selectByIds(ids: NonEmptyList[Long], isBlocked: Boolean = false): ConnectionIO[List[User]] = {
    val idsFr = Fragments.in(fr"id", ids)
    val a = (fr"""SELECT (id, firstName, surname, lastName, email, password, googleId, categoryId, isSuperUser, isBlocked)
          |FROM "User" WHERE""" ++ idsFr ++ fr"""AND isblocked = $isBlocked""")
      .stripMargin
      .query[User]

    println(a)
    a.to[List]
  }

  def testSelect(ids: NonEmptyList[Long], isBlocked: Boolean = false): ConnectionIO[List[Long]] = {
    val idsFr = Fragments.in(fr"id", ids)
    val a = (fr"""SELECT (id)FROM "User" WHERE""" ++ idsFr ++ fr"""AND isblocked = $isBlocked""")
      .query[Long]

    println(a)
    a.to[List]
  }
}

case class User(id: Long,
                firstName: String,
                surname: String,
                lastName: String,
                email: String,
                password: String,
                googleId: Long,
                categoryId: Long,
                isSuperUser: Boolean = false,
                isBlocked: Boolean = false)
