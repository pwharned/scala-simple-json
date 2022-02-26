package database

import java.sql.ResultSet

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.reflect.runtime.universe._


abstract class Table[A: TypeTag](name: String) extends Mapable.CaseMapable[A] {

  implicit val tableName: String = name

  def * : Query[Column[Any]]

  def compose(tables:Table[A]*) = {
    val ctes = tables.map(x =>x.asCte(x.tableName)).mkString(",")
    val query = *.toString
    new GenericTable[A](tableName, *.columns) {
      override def toString: String = "WITH " + ctes + " " + query
    }

  }



  def + (table: Table[A]):GenericTable[A] = {
    val cte = table.asCte(tableName)
    var query: String = toString
    val newColumns = table.*.columns
    if(!query.startsWith("WITH")){
      new GenericTable[A](tableName, newColumns) {
        override def toString: String = "WITH " + cte + " " +  query
      }

    }else{
      query = query.split("SELECT").dropRight(1).mkString("SELECT") + ", " +  table.tableName +" as (SELECT " + query.split("SELECT").last  + ") " + table
      println(query)
      new GenericTable[A](tableName, newColumns) {
        override def toString: String = query
      }
    }

  }

  override def toString: String = *.toString

  def asCte(tableName: String): String = *.asCte(tableName)

  implicit val converter: Mapable.CaseMapable[A] = Mapable.CaseMapable[A]
  //implicit def tupleToQuery(values:  Tuple2[Column[String], Column[String]] ): Query[String] = new Query(values)
  implicit def tupleToQuery[T<:Any](values:  Product ): Query[T] = new Query(values.productIterator.toSeq.asInstanceOf[List[Column[T]]])
  implicit def columnListToQuery[T<:Any](column:  List[Column[T]] ): Query[T] = new Query(column.asInstanceOf[List[Column[T]]])
  implicit def columnToQuery[T<:Any](column:  Column[Any] ): Query[T] = new Query(List(column).toSeq.asInstanceOf[List[Column[T]]])

  type Execution = Future[ResultSet]

  def flatMap[T](implicit connection:AbstractDatabaseConnection[T])  = this.map.apply(this.execute)


  def execute[T](implicit connection: AbstractDatabaseConnection[T]): Future[ResultSet] = {
    connection.execute(toString)
  }

  def map[A](implicit converter: Mapable.CaseMapable[A]  = converter ) = { implicit execution: Execution =>
    execution.map{
      val l = ListBuffer.empty[A]
      result => while(result.next()){
        l+= converter.mapTo(*.columns.map(column => column.retrieve(column.alias,resultSet = result)))

      }
        l
    }
  }


}

class GenericTable[A: TypeTag](name: String, values: List[Column[Any]]) extends Table[A](name){

  def *  = new Query(values).asInstanceOf[Query[Column[Any]]]
}
