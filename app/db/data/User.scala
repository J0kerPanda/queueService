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

  def selectById(id: Int): ConnectionIO[User] = {
    sql"""SELECT "id", firstName, surname, lastName, email, password, googleId, categoryId, isSuperUser, isBlocked
         |FROM "User"
         |WHERE id = $id"""
      .stripMargin
      .query[User]
      .unique
  }

  def selectByIds(ids: NonEmptyList[Int], isBlocked: Boolean = false): ConnectionIO[List[User]] = {
    val idsFr = Fragments.in(fr"id", ids)
    val a = (fr"""SELECT id, firstName, surname, lastName, email, password, googleId, categoryId, isSuperUser, isBlocked
          |FROM "User" WHERE""" ++ idsFr ++ fr"""AND isblocked = $isBlocked""")
      .stripMargin
      .query[User]

    println(a.sql)
    a.to[List]
  }

  def testSelect(ids: NonEmptyList[Long], isBlocked: Boolean = false): ConnectionIO[List[(String, Int, Boolean)]] = {
    val idsFr = Fragments.in(fr"id", ids)
//    val a = (fr"""SELECT (id, firstName, surname, lastName, email, password, googleId, categoryId, isSuperUser, isBlocked) FROM "User" WHERE""" ++ idsFr ++ fr"""AND isblocked = $isBlocked""")
//      .query[(Long, String, String, String, String, String, String, Long, Boolean, Boolean)]

        val a = sql"""SELECT firstName, "id", isblocked FROM "User" WHERE isblocked = $isBlocked"""
          .query[(String, Int, Boolean)]

    println(a.sql)
    a.to[List]
  }
}

case class User(id: Int,
                firstName: String,
                surname: String,
                lastName: String,
                email: String,
                password: String,
                googleId: Int,
                categoryId: Int,
                isSuperUser: Boolean = false,
                isBlocked: Boolean = false)
