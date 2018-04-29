package db.data

import cats.data.NonEmptyList
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie._

object Category {

  def insert(c: Category): ConnectionIO[Int] = {
    sql"""INSERT INTO "Category" ("id", parentid, name, isfinal) VALUES (${c.id}, ${c.parentId}, ${c.name}, ${c.isFinal})"""
      .update()
      .run
  }

  def selectById(id: Int): ConnectionIO[Option[Category]] = {
    sql"""SELECT "id", parentid, name, isfinal FROM "Category" WHERE id = $id"""
      .query[Category]
      .option
  }

  def selectByIds(ids: NonEmptyList[Int]): ConnectionIO[List[Category]] = {
    (fr"""SELECT "id", parentid, name, isfinal FROM "Category" WHERE""" ++ Fragments.in(fr"id", ids))
      .stripMargin
      .query[Category]
      .to[List]
  }
}

case class Category(id: Int, parentId: Option[Int], name: String, isFinal: Boolean)
