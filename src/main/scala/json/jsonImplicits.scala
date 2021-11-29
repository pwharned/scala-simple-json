package json

import json.Json.JsonMap

object JsonImplicits {

  implicit def JsontoJsonMap(Json: Json): JsonMap = Json.asInstanceOf[JsonMap]


}
