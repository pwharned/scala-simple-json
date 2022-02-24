import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import database.{ConcreteDatabaseConfiguration, DatabaseConnection}
import evaluators.ImpactEvaluator

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


object Main extends App {

  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")

  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

  implicit val connection = DatabaseConnection(configuraiton)

  case class Result(prediction: String, group:String)

  val result = new ImpactEvaluator.Impact[Result]("risk", "test_data", "sex", connection = connection)

  def main: Unit = {
    implicit val actorSystem = ActorSystem(Behaviors.empty, "akka-http")

    val route = get {
      path("hello") {

        val res = Await.result(result.result.flatMap, 5.seconds).map(x => json.Json.JsonProduct(x).toString).mkString(",")

        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, res))

      }
    }

    Http().newServerAt("127.0.0.1", 8080).bind(route)

  }
  main

}

