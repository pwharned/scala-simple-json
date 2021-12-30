package database


import json.Json
import json.Json.JsonMap

import scala.io.Source

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


abstract class DatabaseLocator[A] {
  def database: String
}

class DB2DatabaseLocator

abstract class AbstractDatabaseConfiguration(host: String, port: String, username: String, password: String, driver: String, database: String) extends Configuration {



  def connectionString: String = {
    val username = this.username
    val password = this.password
    val host = this.host
    val port = this.port
    val driver = this.getDriver
    val database = this.database
    f"jdbc:$driver%s://$username%s:$password%s@$host%s:$port%s/$database%s"
  }

  def databaseType: String = {
    if(this.driver contains "db2"){
      return "db2"
    }
    else{
     return "db2"
    }
  }

  def isDB2: Boolean = {
  this.databaseType.equals("db2")
  }

  def getDriver: String = {
    if(this.isDB2 ){
      return  "db2"
    }
    else{
      return  "db2"
    }
  }

}

class ConcreteDatabaseConfiguration(host: String, port: String, username: String, password: String, driver: String, database: String) extends AbstractDatabaseConfiguration(host: String, port: String, username: String, password: String, driver: String, database: String) {

  def printhello: String = "hello"

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


object Test extends App {

  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")


  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

  println(configuraiton.connectionString)


}