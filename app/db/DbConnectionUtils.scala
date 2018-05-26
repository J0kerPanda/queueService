package db

import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import javax.inject.{Inject, Singleton}
import javax.sql.DataSource
import play.api.db.Database

@Singleton
class DbConnectionUtils @Inject()(db: Database) {

  def transactor: Aux[IO, DataSource] = Transactor.fromDataSource[IO](db.dataSource)
}
