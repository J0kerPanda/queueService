package controllers.formats

import org.joda.time.LocalDate
import play.api.mvc.QueryStringBindable

import scala.util.Try

object HttpBinders {

  implicit def localDateQueryBinder(implicit sBinder: QueryStringBindable[String]): QueryStringBindable[LocalDate] = new QueryStringBindable[LocalDate] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, LocalDate]] = {

      for {
        dateStr <- sBinder.bind(key, params)
      } yield {
        dateStr match {
          case (Right(str)) => Try(LocalDate.parse(str)).toEither.left.map(_.toString)
          case _ => Left("Unable to bind a Pager")
        }
      }
    }

    override def unbind(key: String, date: LocalDate): String = sBinder.unbind(key, date.toString)
  }
}
