import database.{ConcreteDatabaseConfiguration, DatabaseConnection}
import evaluators.{ExplainabilityEvaluator, ExplanationResult}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object ExplainTest extends App{

  val currentDirectory = new java.io.File(".").getCanonicalPath

  println(currentDirectory +   "/project/database.json")


  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

  implicit val connection = DatabaseConnection(configuraiton)

  /*
  val result = ExplainabilityEvaluator.main("scored_credit", "prediction", features = Seq("loanduration"), max_iter = "10000", learn_rate = ".001",connection = connection)
    val res: Seq[Product] = Await.result(result, 20.seconds).toSeq  print(json.Json(res))
  print(json.Json(res))



   */
val model = new ExplainabilityEvaluator.Explanation[ExplanationResult]( "scored_credit", "prediction", features = Seq("loanduration"), id_column="scoring_id", max_iter = "10000", learn_rate = ".001",connection = connection, ids = Seq(15005))


  val res: Seq[Product] = Await.result(model.retrieve, 5.seconds).toSeq



  println(json.Json(res))


}
