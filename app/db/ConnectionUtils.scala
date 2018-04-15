package db

import javax.inject.{Inject, Singleton}
import javax.sql.DataSource

import cats.effect.IO
import doobie.util.transactor.Transactor
import play.api.db.Database
import doobie.util.transactor.Transactor.Aux

@Singleton
class ConnectionUtils @Inject()(db: Database) {

  def transactor: Aux[IO, DataSource] = Transactor.fromDataSource[IO](db.dataSource)
}
