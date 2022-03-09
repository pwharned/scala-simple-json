import database.{ConcreteDatabaseConfiguration, DatabaseConnection}
import evaluators.{AccuracyDriftEvaluator, DataDriftEvaluator, ExplainabilityEvaluator, ExplanationResult}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object AccuracyDriftTest extends App{

  val currentDirectory = new java.io.File(".").getCanonicalPath

  println(currentDirectory +   "/project/database.json")


  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

  implicit val connection = DatabaseConnection(configuraiton)

  val drift =  AccuracyDriftEvaluator. apply( "scored_credit",  features = Seq("loanduration"), learn_rate = ".001", target = "prediction" ,connection = connection)

  println(drift.compose("hourly"))


  //println(drift. compose2)
  //println(drift. compose3.toString.split("SELECT").dropRight(1).mkString("SELECT")  + f", t1 as (SELECT ${drift. compose3.toString.split("SELECT").last})" )

}
