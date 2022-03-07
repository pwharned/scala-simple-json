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
    val cte = asCte(table.tableName)
    var rightCte = table.asCte(this.tableName)
    var query: String = table.toString
    val newColumns = table.*.columns
    if(!this.toString.startsWith("WITH")){
      new GenericTable[A](table.tableName, newColumns) {
        override def toString: String = "WITH " + cte + " " +  query
        override def asCte(tableName: String): String = table.asCte(tableName)

      }

    }else{
      query = this.toString.split("SELECT").dropRight(1).mkString("SELECT")  + "," + asCte(table.tableName) + " " +  table
      new GenericTable[A](table.tableName, newColumns) {
        override def toString: String = query
      }
    }

  }

  override def toString: String = *.toString

  def asCte(tableName: String): String = *.asCte(tableName)

  implicit val converter: Mapable.CaseMapable[A] = Mapable.CaseMapable[A]
  //implicit def tupleToQuery(values:  Tuple2[Column[String], Column[String]] ): Query[String] = new Query(values)
  implicit def tupleToQuery[T<:Column[Any]](values:  Product ): Query[T] = new Query(values.productIterator.toList.asInstanceOf[List[T]])
  implicit def columnListToQuery[T<:Column[Any]](column:  List[T] ): Query[T] = new Query(column)
  implicit def columnToQuery[T<:Column[Any]](column:  T ): Query[T] = new Query(List(column))

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

  def asModel(target: String, learn_rate: String, max_iter: String):GenericTable[A] = {


    new GenericTable[A](tableName, values = this.*.columns) {

      override def * : Query[Column[Any]] = super.*.queryToModel(target=target, learn_rate=learn_rate, max_iter=max_iter)

      override def map[A](implicit converter: Mapable.CaseMapable[A]  = converter ) = { implicit execution: Execution =>
        execution.map{
          val l = ListBuffer.empty[A]
          result => while(result.next()){
            l+= converter.mapToMap(*.columns.map(column => column.alias ->  column.retrieve(column.alias,resultSet = result).asInstanceOf[Double]).toMap )

          }
            l
        }
      }

      override def +(table: Table[A]):GenericTable[A] = {
        val cte = asCte("model")
        var rightCte = table.asCte(this.tableName)
        var query: String = table.toString
        val newColumns = *.columns

        query =  "WITH " + asCte("model") + "," +  table.asCte("result") + f" SELECT ${newColumns.map(x => f"model.${x}").mkString(",")}, ${table.*.columns.map(x => f"${"result"}.${x}").mkString(",")} from model, result"
          new GenericTable[A](table.tableName, newColumns:::table.*.columns) {
            override def toString: String = query

            override def map[A](implicit converter: Mapable.CaseMapable[A]  = converter ) = { implicit execution: Execution =>
              execution.map{
                val l = ListBuffer.empty[A]
                result => while(result.next()){
                  l+= converter.mapToMap(*.columns.map(column => column.alias ->  column.retrieve(column.alias,resultSet = result).asInstanceOf[Double]).toMap )

                }
                  l
              }
            }
          }
        }

      }




  }


}

class GenericTable[A: TypeTag](name: String, values: List[Column[Any]]) extends Table[A](name){

  def columnValues = values

  def *  = new Query(values)
}
