package database

import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait DatabaseExecutor extends ApplicationConfiguration with Logging {

  def executeStatement(statement: Statement): Future[Boolean] = {

    dbconf.getConnection.map {

      x => {
        x.createStatement().execute(statement.statement)
      }
    }andThen {
      case Success(value) => {logger.debug(f"Successfully executed query ${statement.statement.stripMargin}");value}
      case Failure(exception) => logger.error(f"Error executing statement ${exception.getStackTrace.mkString("\n")}")
    }
  }

  def executeQuery(query: String): Future[Boolean] = {

    dbconf.getConnection.map {

      x => {
        x.createStatement().execute(query)
      }
    } andThen {
      case Success(value) => value
      case Failure(exception) => logger.error(f"Error executing statement ${exception.getStackTrace.mkString("\n")}")
    }
  }

  def execute(statement: Statement): Future[Boolean] = {

    dbconf.getConnection.map {

      x => {
        x.createStatement().execute(statement.statement)
      }
    }
  }


}
