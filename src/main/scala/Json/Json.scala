package Json

import scala.language.implicitConversions
import Json._



object test extends App {



  var jsonMap = Json.fromString("{'hello':[['hello']],'goodbye':{'object2':['hello']}}")

  print(jsonMap)



}
