import java.sql.ResultSet

import Main.result
import database.{ConcreteDatabaseConfiguration, DatabaseConnection, GenericTable, Mapable}
import evaluators.ImpactEvaluator

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

object ImpactEvaluatorTest extends App {



    val currentDirectory = new java.io.File(".").getCanonicalPath

    println(currentDirectory +   "/project/database.json")


    val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

    implicit val connection = DatabaseConnection(configuraiton)

  //case class Result(prediction: String, sex: String, group:String, disparate_impact: String, minutes: String, hours: String, days: String)
  case class Result(prediction: String, sex: String, group:Float,disparate_impact: Double, time:String)


    val resultTable = new ImpactEvaluator.Impact[Result]("risk", "test_data2", "sex", scoring_timestamp = "timestamp",connection = connection)



 // println(cte)

  // println(query)

  //println( ratiosTable.toString.split("SELECT").dropRight(1).mkString("SELECT") + ", " +  resultstable.tableName +" as (SELECT " + ratiosTable.toString.split("SELECT").last  + ") " + resultstable )
  //val res = Await.result(result.result.flatMap, 30.seconds)


 // println(Await.result(resultTable.result.map.apply(resultTable.result.execute), 100.seconds))



  println(resultTable.result.*.columns.map(x =>x.alias) )

  println(Await.result(resultTable.result.flatMap, 5.seconds))




}
