import database.ConcreteDatabaseConfiguration
import json.Json.JsonMap

import scala.io.Source

object JsonTest extends App {
  val currentDirectory = new java.io.File(".").getCanonicalPath

  val path = currentDirectory +   "/project/database.json"
  val fileContents  = Source.fromFile(path).getLines.map(_.trim).mkString("")

  println(fileContents)
val map = JsonMap.fromString(fileContents)

 println( map.toMap)
  //val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

}
