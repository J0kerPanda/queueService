package db.data

import cats.data.NonEmptyList
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.param.Param
import doobie._


object User {

  private type tupleType = User.type

  def insert(u: User): ConnectionIO[Int] = {
    sql"""INSERT INTO "User"
         |(id, firstname, surname, lastname, email, password, googleid, issuperuser, isblocked)
         |VALUES (${u.id}, ${u.firstName}, ${u.surname}, ${u.lastName}, ${u.email},
         |${u.password}, ${u.googleId}, ${u.isSuperUser}, ${u.isBlocked})"""
      .stripMargin
      .update
      .run
  }

  def selectByIds(ids: NonEmptyList[Long], isBlocked: Boolean = false): ConnectionIO[List[User]] = {
    val idsFr = Fragments.in(fr"id", ids)
    (fr"""SELECT * FROM "User" WHERE""" ++ idsFr ++ fr"""AND isblocked = $isBlocked""")
      .query[User]
      .to[List]
  }
}

case class User(id: Long, firstName: String, surname: String,
                lastName: String, email: String, password: String,
                googleId: Long, isSuperUser: Boolean = false, isBlocked: Boolean = false)
