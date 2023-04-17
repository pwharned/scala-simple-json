package database

import database.ResultSetStream.ResultSetStream
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}




trait ApplicationInitializer extends DatabaseExecutor{

  def initialze(): Unit = {

    executeStatement(CreateTransactionsTableStatement)

    executeStatement(CreateModelsTableStatement)
  }




}