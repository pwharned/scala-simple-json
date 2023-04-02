import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.{complete, pathPrefix}
import org.slf4j.LoggerFactory
import spray.json._



object Main extends App with DefaultJsonProtocol {



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


  val test_json =
    """[{"target":"male","datapoints":[[0.7],[0.4],
      |[0.9],[1.2]]},{"target":"female","datapoints":[[1.7],
      |[1.4], [1.3], [1.1]]}]""".stripMargin


  def updateTestJson(test_json: String): String = {

    test_json.parseJson.convertTo[Array[GrafanaTarget]].array.map{

      x => new GrafanaTarget( x.target, x.datapoints.map(y =>  GrafanaDataPoint(y.arr ++ Array( (System.currentTimeMillis()).toDouble) )  ))
    }
  }.toJson.toString

  updateTestJson(test_json)

 // println(test_json.parseJson.convertTo[Array[GrafanaTarget]])

  //println(test_json.parseJson.convertTo[JsArray].elements.map(x =>x.asJsObject.getFields("datapoints", "target").map(x =>x.getClass)))

  val search_json = """["male","female"]""".stripMargin
  val logger = LoggerFactory.getLogger("Main")
  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")
  implicit val system = ActorSystem(Behaviors.empty, "my-system")
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.executionContext


  val route =
    pathPrefix("") {
concat(
  get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      },
  post {
    complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
  }
)}

  val grafanaQuery =
    concat(path("grafana" / "search") {
      concat(
        get {
          complete(HttpEntity(ContentTypes.`application/json`, search_json))
        },
        post {
          complete(HttpEntity(ContentTypes.`application/json`, search_json))
        }
      )
    },
  path("grafana" / "query") {
    concat(
      get {
        complete(HttpEntity(ContentTypes.`application/json`, updateTestJson(test_json)))
      },
      post {
        complete(HttpEntity(ContentTypes.`application/json`, updateTestJson(test_json)))
      }
    )
  })

  val routes = grafanaQuery~route

    def main = Http().newServerAt("127.0.0.1", 8080).bind(routes)



    main


}

