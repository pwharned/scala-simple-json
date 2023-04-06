import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.{complete, pathPrefix}
import spray.json._

import scala.util.Random
import com.typesafe.config.{Config, ConfigFactory}
import database.DatabaseConnection
import database.ResultSetStream.ResultSetStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.ResultSet
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Main extends App with DefaultJsonProtocol {

  val logger = LoggerFactory.getLogger(classOf[App])



  val conf: Config = ConfigFactory.load("application.conf");
  val dbconf: DatabaseConnection = new DatabaseConnection(conf)

  val result: Future[Stream[ResultSet]] = dbconf.getConnection.map {

    x => {
      val statement = x.createStatement()
      val result = statement.executeQuery("SELECT 1 from sysibm.sysdummy 1")

      result.toStream

    }
  }
  case class GrafanaTarget(target:String, datapoints: Array[GrafanaDataPoint])


  case class GrafanaDataPoint(arr: Array[Double])

  implicit object GrafanaDatapointJsonFormat extends RootJsonFormat[GrafanaDataPoint]{
    override def read(json: JsValue): GrafanaDataPoint = json match {
      case JsArray(num) => GrafanaDataPoint(json.convertTo[Array[Double]])
    }

    override def write(obj: GrafanaDataPoint): JsValue = obj.arr.toJson
  }

  implicit object GrafanaDataPointArrayJsonFormat extends RootJsonFormat[Array[GrafanaDataPoint]]{
    override def read(json: JsValue): Array[GrafanaDataPoint] = json match{
      case JsArray(elements) => elements.map(x => x.convertTo[GrafanaDataPoint]).toArray
    }

    override def write(obj: Array[GrafanaDataPoint]): JsValue = obj.map( x=> x.toJson).toJson
  }

  implicit val grafanaTargetJsonFormat = jsonFormat2(GrafanaTarget)


 // println("""[[0.7],[0.4],
  //  |[0.9],[1.2]]""".stripMargin.parseJson.convertTo[Array[JsValue]].map(x => x.convertTo[GrafanaDataPoint]))

//  println("""[[0.7],[0.4],
  //          |[0.9],[1.2]]""".stripMargin.parseJson.convertTo[Array[GrafanaDataPoint]])

/*
  val test_json =
    """[{"target":"male","datapoints":[[0.7],[0.4],
      |[0.9],[1.2]]},{"target":"female","datapoints":[[1.7],
      |[1.4], [1.3], [1.1]]}]""".stripMargin
*/
  def test_json =
    f"""[{"target":"male", "datapoints":${(0 to 100).map(x => Array(Random.nextFloat(), (System.currentTimeMillis().toDouble-(Random.nextInt(60)*6000)).toDouble ) ).toJson}},
       |{"target":"female", "datapoints":${(0 to 100).map(x => Array(Random.nextFloat(), (System.currentTimeMillis().toDouble-(Random.nextInt(60)*6000)).toDouble ) ).toJson}}
       |]""".stripMargin

  def updateTestJson(test_json: String): String = {

    test_json.parseJson.convertTo[Array[GrafanaTarget]].array.map{

      x =>  GrafanaTarget( x.target, x.datapoints.map(y =>  GrafanaDataPoint(y.arr ++ Array( (System.currentTimeMillis()).toDouble) )  ))
    }
  }.toJson.toString



  val search_json = """["male","female"]""".stripMargin
  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")
  implicit val system = ActorSystem(Behaviors.empty, "my-system")
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.executionContext


  val route =
    pathPrefix("") {
concat(
  get {
    logger.info(f"Handling request for root ")
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      },
  post {
    logger.info(f"Handling request for root ")
    complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
  }
)}

  val grafanaQuery =
    concat(path("grafana" / "search") {
      concat(
        get {
          logger.info(f"Handling request for 'grafana/search' ")

          complete(HttpEntity(ContentTypes.`application/json`, search_json))
        },
        post {
          logger.info(f"Handling request for 'grafana/search' ")

          complete(HttpEntity(ContentTypes.`application/json`, search_json))
        }
      )
    },
  path("grafana" / "query") {
    concat(
      get {
        complete(HttpEntity(ContentTypes.`application/json`, test_json))
      },
      post {
        complete(HttpEntity(ContentTypes.`application/json`, test_json))
      }
    )
  })

  val routes = grafanaQuery~route

    def main = Http().newServerAt("127.0.0.1", 8080).bind(routes)



    main


}

