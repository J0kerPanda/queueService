package db.data

import cats.data.NonEmptyList
import db.DatabaseFormats.IdEntity
import db.data.Category.CategoryId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._

object Category {

  type CategoryId = Int

  def insert(c: CategoryData): ConnectionIO[CategoryId] = {
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
      .query[Category]
      .to[List]
  }
}

case class CategoryData(parentId: Option[CategoryId], name: String, isFinal: Boolean)

case class Category(id: CategoryId, data: CategoryData) extends IdEntity[CategoryId, CategoryData]
