package database
import java.sql.ResultSet

import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Expression[+T] extends Queryable[T] {
  def alias[T](aliasedName: String)(implicit column: Column[T]): Column[T] => Column[T] = ???
}

trait Aggregation[+T] extends Expression[T] {

  def agg[T](aliasedName: String, aggregation: String )(implicit column: Column[T]):Column[T] => Column[T] = {
    val columnName = column.expression
    implicit column : Column[T] =>
      Column( columnName, aliasedName, aggregation = aggregation )


  }
  def aggOver[T](aliasedName: String, aggregation: String, columns: Column[T]* )(implicit column: Column[T]):Column[T] => Column[T] = {
    val columnName = column.expression
    implicit column : Column[T] =>
      Column( columnName, aliasedName, aggregation = aggregation, over = columns:_*)


  }



}


trait Cast[+T] extends Expression[T] {

  def castTo[T](dtype: String )(implicit column: Column[T]):Column[T] => Column[T] = {
    val columnName = column.expression
    implicit column : Column[T] =>
    Column.apply( column, datatype = dtype)


  }
}


trait Count[+T] extends Aggregation[T] {


  override def alias[T](aliasedName: String)(implicit column: Column[T]) = {
    val columnName = column.columnName
    implicit column : Column[T] =>
      Column( columnName, aliasedName )


  }

  def countAs[T](aliasedName: String)(implicit column: Column[T]):Column[T] => Column[T] = agg(aliasedName, "COUNT")




}

trait Alias[+T] extends Expression[T] {



  override def alias[T](aliasedName: String)(implicit column: Column[T]) = {
    val columnName = column.columnName
    implicit column : Column[T] =>
      Column( columnName, aliasedName )


  }

}


class Column[+T<:Any](name: String)(implicit retriever: Queryable[T]) extends Aggregation[T] with Alias[T] with Count[T] with Cast[T]  {
  override def toString: String = name

  def alias: String = name
  def expression: String = name

  implicit val column: Column[T] = this
  implicit val columnName: String = name
  def as[A>:T](aliasedName: String =name)(implicit column: Column[A] = this) = super.alias(aliasedName).apply(this)
  def count[A>:T](aliasedName: String =name)(implicit column: Column[A] = this) = super.countAs(aliasedName).apply(this)
  def aggregate[A>:T](aliasedName: String =name, aggregation: String)(implicit column: Column[A] = this) = super.agg(aliasedName,aggregation ).apply(this)
  def aggregateOver[A>:T](aliasedName: String, aggregation: String, overColumns: Column[A]*)(implicit column: Column[A] = this) = super.aggOver(aliasedName,aggregation, overColumns:_* ).apply(this)

  def unpack: Column[T] = Column(columnName)

  def /[A>:T](div: Column[A]): Column[T] ={
    val divisor = div.toString
    val numerator = this.toString
    new Column[T](name=column.columnName){
      override def toString = numerator + "/" + divisor
    }

  }

  def cast[A>:T](datatype: String )(implicit column: Column[A] = this) = super.castTo( dtype = datatype).apply(this)


  def retrieve(columnName: String,resultSet: ResultSet): Any = retriever.retrieve(columnName, resultSet)
  def retrieve(columnName: String, resultSet: Future[ResultSet]): Future[Any] = resultSet.map(result => retriever.retrieve(columnName, resultSet))


}

object Column {

  def apply[T](name: String)(implicit retriever: Queryable[T]): Column[T] = new Column[T](name=name)

  def apply[T](name: String, columnAlias: String)(implicit retriever: Queryable[T]): Column[T] = new Column[T](name=name){
    override def toString: String = f"$name as $columnAlias"
    override def alias: String = columnAlias
  }


  def apply[T](name: String, columnAlias: String, aggregation: String)(implicit retriever: Queryable[T]): Column[T] = new Column[T](name=name){
    override def toString: String = f"$aggregation($name) as $columnAlias"
    override def alias: String = columnAlias
    override def expression: String = f"$aggregation($name)"
  }
  def apply[T](name: String, columnAlias: String, aggregation: String, over: Column[T]*)(implicit retriever: Queryable[T]): Column[T] = new Column[T](name=name){
    override def toString: String = f"$expression as $columnAlias"
    override def alias: String = columnAlias
    override def expression: String = f"$aggregation($name) OVER( PARTITION BY ${over.map(x => x.columnName).mkString(",")} )"
  }
  def apply[T](column: Column[T], datatype: String )(implicit retriever: Queryable[T]): Column[T] = new Column[T](name=column.columnName){
    override def toString: String = f"cast(${column.columnName} as $datatype)"
    override def expression: String = f"cast(${column.columnName} as $datatype)"
  }

  def apply[T](column: Column[T])(implicit retriever: Queryable[T]): Column[T] = new Column[T](name=column.columnName)



  //def apply[T](value: T)(implicit converter:Queryable[T]): Column[T] = converter.toQueryable(value)
}


trait Queryable[+T]{
  def retrieve( columnName: String,resultSet: ResultSet): Any
  def retrieve(columnName: String, resultSet: Future[ResultSet]): Future[Any]

}

object Queryable{
  implicit object StringQueryable extends Queryable[String]{
    override def retrieve(columnName: String, resultSet: ResultSet):String  = resultSet.getString(columnName)
    override def retrieve(columnName: String, resultSet: Future[ResultSet]): Future[String]  = resultSet.map(x =>x.getString(columnName))

  }
  implicit object IntQueryable extends Queryable[Int]{
    override def retrieve(columnName: String, resultSet: ResultSet):Int  = resultSet.getInt(columnName)
    override def retrieve(columnName: String, resultSet: Future[ResultSet]): Future[Int]  = resultSet.map(x =>x.getInt(columnName))
  }
}

trait Mapable {
  val classLoaderMirror = runtimeMirror(getClass.getClassLoader)

  class CaseMapable[T:TypeTag]{


    val tpe = typeOf[T]
    val classSymbol = tpe.typeSymbol.asClass
    println(classSymbol)

    if (!(tpe <:< typeOf[Product] && classSymbol.isCaseClass)) {
      print(tpe.toString)
      throw new IllegalArgumentException(
        "CaseClassFactory only applies to case classes!"
      )
    }

    val classMirror = classLoaderMirror reflectClass classSymbol

    val constructorSymbol = tpe.declaration(nme.CONSTRUCTOR)

    val defaultConstructor =
      if (constructorSymbol.isMethod) constructorSymbol.asMethod
      else {
        val ctors = constructorSymbol.asTerm.alternatives
        ctors.map { _.asMethod }.find { _.isPrimaryConstructor }.get
      }

    val constructorMethod = classMirror reflectConstructor defaultConstructor

    /**
     * Attempts to create a new instance of the specified type by calling the
     * constructor method with the supplied arguments.
     *
     * @param args the arguments to supply to the constructor method
     */
    def mapTo(args: Seq[_]): T = constructorMethod(args: _*).asInstanceOf[T]

  }


  object CaseMapable{
    def apply[T: TypeTag]: CaseMapable[T] = new CaseMapable[T]
  }

}

object Mapable extends Mapable


abstract class Table[A: TypeTag](name: String) extends Mapable.CaseMapable[A] {

  implicit val tableName: String = name

  def * : Query[Column[Any]]



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



class Query[+T<:Any] (values: List[Column[T]] )(implicit tableName: String) {




  def drop[A<:Column[_]](column: A*): Query[T] = new Query(values.filter( x=> !column.map(c=>c.columnName).contains(x.columnName)))


  def columns: List[Column[Any]] = values

  def cl[T]: String = columns.map( x=>x match {
    case x: Column[T] => x.toString
  }).mkString(",")

  override def toString: String = f"SELECT $cl FROM $tableName"

  def asCte(tableName: String): String = f"WITH $tableName as (" + toString + ")"


  def select[A<:Column[_]](column: A*): Query[T] = new Query(values.filter(x => column.map(c => c.columnName).contains(x.columnName )))


  def groupBy[A<: Any](groupByColumns: Column[A]*): Query[T] = {
    val query: String = this.toString
     new Query(values)
    {
      override def toString = query + (this :: Mutatable.GroupAble).clause(groupByColumns.map(x =>x.expression):_* ).apply(groupByColumns.map(x => x.expression).mkString(","))

    }
  }

  def filter[A<: Any](filterColumn: Column[A], operator: String, value: String ): Query[T] = {
    val condition =  Condition(filterColumn, operator, value)
    val query: String = this.toString
    new Query(values)
    {
      override def toString = query + (this :: Mutatable.Filterable).clause(condition ).apply(filterColumn.expression)

    }
  }

  def filter[A<: Any, B: TypeTag](filterColumn: Column[A], operator: String, table: Table[B] ): Query[T] = {
    val condition =  Condition(filterColumn, operator, table)
    val query: String = this.toString
    new Query(values)
    {
      override def toString = query + (this :: Mutatable.Filterable).clause(condition ).apply(filterColumn.expression)

    }
  }


}





object Query{

  def apply[T <: Any](values: List[Column[T]])(implicit tableName: String): Query[T] = new Query(values)


}




object ColumnTest extends  App{

  case class Result(column_name: String, data_type: String)

  //def mapTo[R <: Product with Serializable](implicit rCT: TypeTag[R]): TypeTag[R] = rCT

  //implicit def typTagConverter(arg: Result):  TypeTag[Result] = mapTo[Result]

  class ResultTable extends Table[Result](name= "result") {
    def column_name = Column[String]("COLUMN_NAME")
    def data_type = Column[String](name = "DATA_TYPE")

    def * =  ( data_type,column_name)
  }


  val table  = new ResultTable
val columns = Seq("COLUMN_NAME")
}

///Map to shoudl return a function which implicitly maps to a collection of the case class