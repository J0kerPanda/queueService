package db

import javax.inject.{Inject, Singleton}

import doobie.util.transactor.Transactor
import play.api.Configuration

@Singleton
class Connection @Inject() (config: Configuration) {

//  private val dbName = config.get[Configuration]("db")
//
//  def transactor =
//  val xa = Transactor.fromDataSource.apply
}
