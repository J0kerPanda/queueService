package controllers

import controllers.HttpFormats._
import db.{ConnectionUtils, DatabaseUtils}
import db.data.Category
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class CategoryController @Inject()(cu: ConnectionUtils, cc: ControllerComponents) extends AbstractController(cc) {

  def create(name: String, isFinal: Boolean) = Action {
    val category = Category.forInsertion(None, name, isFinal)

    val tr: ConnectionIO[scala.Option[Category]] = for {
      _ <- Category.insert(category)
      id <- DatabaseUtils.returnLastIntId
      c <- Category.selectById(id)
    } yield c

    Created(tr.transact(cu.transactor).unsafeRunSync().toJson)
  }

  def get(id: Int) = Action {
    Ok(Category.selectById(id).transact(cu.transactor).unsafeRunSync().toJson)
  }
}