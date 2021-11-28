package database


import json.Json
import json.Json.JsonMap

import scala.io.Source

trait Configuraiton

abstract class AbstractDatabaseConfiguration(host: String, port: String, username: String, password: String, driver: String) extends Configuraiton {

}

class ConcreteDatabaseConfiguration(host: String, port: String, username: String, password: String, driver: String) extends AbstractDatabaseConfiguration(host: String, port: String, username: String, password: String, driver: String)


object ConcreteDatabaseConfiguration{

  def apply(host: String, port: String, username: String, password: String, driver: String): ConcreteDatabaseConfiguration = new ConcreteDatabaseConfiguration(host, port, username, password, driver)

  def apply(map: Map[String, String]): ConcreteDatabaseConfiguration = {

    new ConcreteDatabaseConfiguration(host = map("host"), port =  map("port"), password = map("password"), username = map("username"), driver = map("driver"))
  }

  def apply(path: String): ConcreteDatabaseConfiguration = {
    val fileContents  = Source.fromFile(path).getLines.map(_.trim).mkString("")

    val args = JsonMap.fromString(fileContents).toMap

    return ConcreteDatabaseConfiguration(args)

  }
}


object Test extends App {

  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")


  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

  print(configuraiton)


}