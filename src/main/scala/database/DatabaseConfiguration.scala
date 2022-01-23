package database


import java.lang.Class

import json.Json
import json.Json.JsonMap
import database.Driver

import scala.io.Source
import java.sql.{Connection, DriverManager, ResultSet}

import database.ColumnTest.Result

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.reflect.runtime.universe.TypeTag

trait Configuration {


}

object Configuration {
  def fromJson(string: String): Map[String, String] = {
    JsonMap.fromString(string).toMap

  }

  def fromFile(path: String): Map[String, String] = {
    val fileContents  = Source.fromFile(path).getLines.map(_.trim).mkString("")
    this.fromJson(fileContents)

  }

  def apply(path: String): Map[String, String] = {
    this.fromFile(path)
  }
}



abstract class AbstractDatabaseConfiguration(host: String, port: String, username: String, password: String, driver: String, database: String) extends Configuration {

  def getUsername: String = this.username

  def getPassword: String = this.password

  def connectionString: String = {
    val username = this.username
    val password = this.password
    val host = this.host
    val port = this.port
    val driver = this.getDriver.name
    val database = this.database
    f"jdbc:$driver%s://$host%s:$port%s/$database%s"
  }


  def getDriver: Driver = {
    Driver(this.driver)

  }

  def getType: String = {
    Driver(this.driver).name
  }

}

class ConcreteDatabaseConfiguration(host: String, port: String, username: String, password: String, driver: String, database: String) extends AbstractDatabaseConfiguration(host: String, port: String, username: String, password: String, driver: String, database: String) {


}


object ConcreteDatabaseConfiguration extends Configuration {

  def apply(host: String, port: String, username: String, password: String, driver: String, database: String): ConcreteDatabaseConfiguration = new ConcreteDatabaseConfiguration(host, port, username, password, driver, database)

  def apply(map: Map[String, String]): ConcreteDatabaseConfiguration = {

    new ConcreteDatabaseConfiguration(host = map("host"), port =  map("port"), password = map("password"), username = map("username"), driver = map("driver"), database = map("database"))
  }

  def apply(path: String): ConcreteDatabaseConfiguration = {
    ConcreteDatabaseConfiguration(Configuration(path))

  }
}

sealed trait DB2

abstract class AbstractDatabaseConnection[A](configuration: AbstractDatabaseConfiguration) {

  def connection: Future[Connection] = Future {
    //Class.forName( configuration.getDriver.toString)
    print(configuration.connectionString)
    DriverManager.getConnection(configuration.connectionString, configuration.getUsername, configuration.getPassword )

  }

  def execute(query: String): Future[ResultSet] = {
    val connection = this.connection
    connection.map(x => x.createStatement().executeQuery(query) )
  }



  def getTable(tableName: String): mutable.Map[String, String] = {
    val resultSet = this.connection.map(x => x.createStatement().executeQuery(f"select COLUMN_NAME from sysibm.columns where table_name= \'$tableName\' ") )
    val mymap =  collection.mutable.Map[String, String]()

    resultSet.foreach(x => while(x.next()){
      mymap.addOne( (x.getString("COLUMN_NAME"), x.getString("DATA_TYPE") ) )

    }  )

     mymap
  }

}

class DB2DatabaseConnection(configuration: AbstractDatabaseConfiguration) extends AbstractDatabaseConnection[DB2](configuration: AbstractDatabaseConfiguration)


object DatabaseConnection {
  def apply(configuration: AbstractDatabaseConfiguration): AbstractDatabaseConnection[DB2] = configuration.getType match {
    case "db2" => new DB2DatabaseConnection(configuration)
  }
}






object Test extends App {

  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")
 // java.sql.DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver);


 val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")
var query = "SELECT COLUMN_NAME, DATA_TYPE FROM SYSIBM.COLUMNS where table_name = 'SYSTABLES'"

  val connection = DatabaseConnection(configuraiton)

val result = Await.result(connection.execute(query), 2.seconds)

  case class Result(column_name: String, data_type: String)

  class ResultTable extends Table[Result](name= "result") {
    def column_name = Column[String]("COLUMN_NAME")
    def data_type = Column[String](name = "DATA_TYPE")

    def * =  (column_name, data_type)
  }


  val table  = new ResultTable
  //println(table.*.mapTo[Result](resultSet = result ))
  val columns = table.*.columns
  var mylist: Result = Result("hello", "goodbye")

while(result.next()){

  Mapable.CaseClassFactory.apply[Result](columns.map(x => x.retrieve(result)))
}


  //println(table.*.mapTo[Result](resultSet = result ))


}