package Json

import scala.util.{Try, Success, Failure}

sealed trait Json

object Json {

  val comma = ",(?![^\\{|\\[]*[\\}|\\]^])"
  //val colin = ":(?![^\\{|\\[]*[\\}|\\]^])"

  val colin = ":(?![^\\{]*[\\}^])"

  case class JsonString( value: String) extends Json {
    override def toString: String = value
  }
  case class JsonNumber(value: Double) extends Json{
    override def toString: String = value.toString
  }
  case class JsonBoolean(value: Boolean) extends Json{
    override def toString: String = value.toString
  }
  case class JsonNull(value: Null) extends Json{
    override def toString: String = value
  }
  case class JsonList(items: Json*) extends Json {
    override def toString: String = "[" + items.mkString(",") + "]"
  }

  case class JsonMap(items: Map[String,Json]) extends Json{
    override def toString: String = "{" +  items.map{ case (k->v) => k.toString + ":" + v.toString   }.mkString(",") + "}"
  }

  object JsonMap{
    def fromString(x: String): JsonMap = {




      val map =  x.stripPrefix("{").stripSuffix("}").  split(Json.comma).map(_.split(Json.colin)).map {

        case Array(k, v)   => (k, Json.fromString(v) )

      }.toMap
      JsonMap(map)
    }


  }

  object JsonList {
    def fromString(x: String): JsonList = {
      val seq = x.stripPrefix("[").stripSuffix("]").split(Json.comma).toSeq.map(x => Json.fromString(x))
      JsonList(seq:_*)

    }



  }

  def apply[T](x:T)(implicit converter: JsonValue[T]): Json = converter.serialize(x)

  def fromString(y: String): Json = {
    val x = y.trim()
    //println(x)
    x match {
    case x if x.startsWith("[") => {   JsonList.fromString(x)}
    case x if x.startsWith("{") => {  JsonMap.fromString(x) }
    case "null" => new JsonNull(value = null)
    case "True" => new JsonBoolean(true)
    case "False"=> new JsonBoolean(false)
    case x if Try{x.toFloat}.isSuccess => new JsonNumber(value = x.toDouble)
    case x: String => JsonString(x)
  }}



 }

trait JsonValue[T] {

  def serialize(t: T): Json

}

object JsonValue{
  implicit object StringJsonValue extends JsonValue[String]{
    def serialize(t: String): Json.JsonString = Json.JsonString(t)
  }
  implicit object NumJsonValue extends JsonValue[Double]{
    def serialize(t: Double): Json.JsonNumber = Json.JsonNumber(t)
  }
  implicit object BooleanJsonValue extends JsonValue[Boolean]{
    def serialize(t: Boolean): Json.JsonBoolean = Json.JsonBoolean(t)
  }

  implicit object NullJsonValue extends JsonValue[Null]{
    def serialize(t: Null): Json.JsonNull = Json.JsonNull(t)
  }




  implicit def MapJsonValue[T: JsonValue]: JsonValue[Map[String, T]] = new JsonValue[Map[String, T]]{
    def serialize(t:  Map[String, T]):Json.JsonMap = {
      Json.JsonMap(t.map(t => (t._1, implicitly[JsonValue[T]].serialize(t._2) )    ))
    }
  }

  implicit def SeqJsonValue[T: JsonValue]: JsonValue[Seq[T]] = new JsonValue[Seq[T]]{
    def serialize(t: Seq[T]):Json.JsonList = {
      Json.JsonList(t.map(implicitly[JsonValue[T]].serialize):_*)
    }
  }




}

