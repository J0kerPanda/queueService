package db.data

import cats.data.NonEmptyList
import db.data.Category.CategoryId
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie._

object Category {

  type CategoryId = Int

  def insert(c: Category): ConnectionIO[CategoryId] = {
    sql"""INSERT INTO "Category" (parentid, name, isfinal) VALUES (${c.parentId}, ${c.name}, ${c.isFinal})"""
      .update()
      .withUniqueGeneratedKeys[CategoryId]("id")
  }

  def selectById(id: CategoryId): ConnectionIO[Option[Category]] = {
    sql"""SELECT id, parentid, name, isfinal FROM "Category" WHERE id = $id"""
      .query[Category]
      .option
  }

  def selectByIds(ids: NonEmptyList[CategoryId]): ConnectionIO[List[Category]] = {
    (fr"""SELECT id, parentid, name, isfinal FROM "Category" WHERE""" ++ Fragments.in(fr"id", ids))
      .stripMargin
      .query[Category]
      .to[List]
  }

  def forInsertion(parentId: Option[CategoryId], name: String, isFinal: Boolean): Category = {
    Category(-1, parentId, name, isFinal)
  }
}

case class Category(id: CategoryId, parentId: Option[CategoryId], name: String, isFinal: Boolean)
