import database.{ConcreteDatabaseConfiguration, DatabaseConnection}
import evaluators.{DataDriftEvaluator, ExplainabilityEvaluator, ExplanationResult}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object DriftTest extends App{

  val currentDirectory = new java.io.File(".").getCanonicalPath

  println(currentDirectory +   "/project/database.json")


  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

  implicit val connection = DatabaseConnection(configuraiton)

  val drift =  DataDriftEvaluator.main( "scored_credit",  features = Seq("loanduration"),measure = "avg", over = "hourly",connection = connection)



print(Await.result(drift, 10.seconds))


}
