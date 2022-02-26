import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import database.{ConcreteDatabaseConfiguration, DatabaseConnection}
import evaluators.ImpactEvaluator
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import spray.json.DefaultJsonProtocol

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


object Main extends App {

  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")

  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

  implicit val connection = DatabaseConnection(configuraiton)

  trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val itemFormat = jsonFormat4(Request)
  }

  case class Result(prediction: String, sex: String, group:Float,disparate_impact: Double, minutes: Int, hours: Int, days: Int)

  case class Request(prediction: String, table: String, protected_column: String, scoring_timestamp: String )

  val result = new ImpactEvaluator.Impact[Result]("risk", "test_data2", "sex", "timestamp",connection = connection)

  class Application extends Directives with JsonSupport {

    implicit val actorSystem = ActorSystem(Behaviors.empty, "akka-http")


    val route = post {
      path("api" / "disparate_impact") {

            entity(as[Request]){
              request =>

                val result = new ImpactEvaluator.Impact[Result](request.prediction, request.table, request.protected_column, request.scoring_timestamp,connection = connection)


                val res: Seq[Product] = Await.result(result.result.flatMap, 5.seconds).toSeq


                complete(HttpEntity(ContentTypes.`application/json`, json.Json(res).toString))
            }

      }
    }

def main = Http().newServerAt("127.0.0.1", 8080).bind(route)

  }

  val server = new Application
  server.main


}

