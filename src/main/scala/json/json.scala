package json

import scala.util.{Try, Success, Failure}

 trait Json

object Json {

  val comma = ",(?![^\\{|\\[]*[\\}|\\]^])"

  //val colin = ":(?![^\\{]*[\\}^])"

  val colin = ":(?!\\w*\")(?![^\\{]*[\\}^])(?<!\\w*\")"
  //https://stackoverflow.com/questions/1443360/regex-for-matching-a-character-but-not-when-its-enclosed-in-quotes

  case class JsonString( value: String) extends Json {
    override def toString: String = value.replaceAll("\"", "")
  }


//  case class JsonNumber[T](value: T ) extends Json{
 //   override def toString: String = value.toString
  //}

  sealed trait JsonNumber[T]

  case class JsonInt(value: Int) extends Json with JsonNumber[Int]{
    override def toString: String = value.toString
  }

  case class JsonAny(value: Any) extends Json {

    override def toString: String = value match {
      case x: Int => JsonInt(value.asInstanceOf[Int]).toString
      case x: String => JsonString(value.asInstanceOf[String]).toString
      case _  => value.toString
    }
  }

  case class JsonDouble(value: Double) extends Json with  JsonNumber[Double]{
    override def toString: String = value.toString
  }

  case class JsonProduct(value: Product) extends Json {
    override def toString: String = "[" + value.productIterator.toArray.map(x => Json.JsonAny(x)).mkString(",") +"]"
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

  case class JsonMap(val items: Map[String,Json]) extends Json{
    override def toString: String = "{" +  items.map{ case (k->v) => k + ":" + v.toString   }.mkString(",") + "}"

    def toMap: Map[String, String] = items.map{
      case (k -> v) => k -> v.toString
    }

    def get(key: String): Option[Json] = items.get(key)
  }

  object JsonMap {
    def fromString(x: String): JsonMap = {
      var map =  x.trim.stripPrefix("{").stripSuffix("}").trim.split(Json.comma).map(_.split(Json.colin)).map {

        case Array(k, v)   => (k.replaceAll("\"", "").trim , Json.fromString(v.replaceAll("\"", "").trim) )

      }.toMap
      JsonMap(map)
    }


  }

  object JsonList {

    def fromString(x: String): JsonList = {
      val seq = x.trim.stripPrefix("[").stripSuffix("]").trim.split(Json.comma).toSeq.map(x => Json.fromString(x))
      JsonList(seq:_*)

    }

  }

  def apply[T](x:T)(implicit converter: JsonValue[T]): Json = converter.serialize(x)

  def fromString(y: String)  = {
    val x = y.trim()
    //println(x)
    x match {
      case x if x.startsWith("[") => {   JsonList.fromString(x)}
      case x if x.startsWith("{") => {  JsonMap.fromString(x) }
      case "null" =>  JsonNull(value = null)
      case "True" =>  JsonBoolean(true)
      case "False"=>  JsonBoolean(false)
      case x if x.matches("[0-9]+") => JsonInt(value = x.toInt)
      case x if Try{x.toDouble}.isSuccess =>  JsonDouble(value = x.toDouble  )
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
  implicit object DoubleJsonValue extends JsonValue[Double]{
    def serialize(t: Double): Json.JsonDouble = Json.JsonDouble(t)
  }
  implicit object IntJsonValue extends JsonValue[Int]{
    def serialize(t: Int): Json.JsonInt = Json.JsonInt(t)
  }
  implicit object BooleanJsonValue extends JsonValue[Boolean]{
    def serialize(t: Boolean): Json.JsonBoolean = Json.JsonBoolean(t)
  }
  implicit object NullJsonValue extends JsonValue[Null]{
    def serialize(t: Null): Json.JsonNull = Json.JsonNull(t)
  }
  implicit object AnyJsonValue extends JsonValue[Any]{
    def serialize(t: Any): Json.JsonAny = Json.JsonAny(t)
  }
  implicit object ProductJsonValue extends JsonValue[Product]{
    def serialize(t: Product): Json.JsonProduct = Json.JsonProduct(t)
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


import scala.language.implicitConversions
import Json._



object test extends App {



  var jsonMap = Json.fromString(
    """
      {
       "username": "vly34104",
        "port": "31321",
        "host": "ba99a9e6-d59e-4883-8fc0-d6a8c9f7a08f.c1ogj3sd0tgtu0lqde00.databases.appdomain.cloud:10",
        "password": "7LKhm8kHnNOegkZa",
        "database":  "bludb",
        "driver": "com.ibm.db2.jcc4"
      }"""
  )


  case class Person(name: String)
  val myperson =  Person("john")
  val personSeq: Seq[Product] = Seq(myperson)

  print(Json.JsonProduct(myperson).toString)
  print(jsonMap)




}
