package database
import java.sql.ResultSet

import database.Mapable.getClass

import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import scala.collection.mutable.ArrayBuffer

class Column[+T<:Any](name: String)(implicit retriever: Queryable[T]) {
  override def toString: String = name

  def retrieve(resultSet: ResultSet): Any = retriever.retrieve(name, resultSet)
}

object Column {

  def apply[T](name: String)(implicit retriever: Queryable[T]): Column[T] = new Column[T](name=name)

  //def apply[T](value: T)(implicit converter:Queryable[T]): Column[T] = converter.toQueryable(value)
}


trait Queryable[+T]{
   def retrieve(column_name: String, resultSet: ResultSet): Any
}

object Queryable{
  implicit object StringQueryable extends Queryable[String]{
    override def retrieve(column_name: String, resultSet: ResultSet):String  = resultSet.getString(column_name)
  }
  implicit object IntQueryable extends Queryable[Int]{
    override def retrieve(column_name: String, resultSet: ResultSet):Int = resultSet.getInt(column_name)
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





}


object Mapable extends Mapable{

  object CaseClassFactory{
    def apply[T: TypeTag](args: Seq[_]): T = new CaseMapable[T].mapTo(args)
  }
}

abstract class Table[A](name: String) {



  def * : Query[Column[Any]]

  //implicit def tupleToQuery(values:  Tuple2[Column[String], Column[String]] ): Query[String] = new Query(values)
  implicit def tupleToQuery[T<:Any](values:  Product ): Query[T] = new Query(values)

  def select: String = {
    val columns = *.toString
    f"SELECT $columns FROM $name"
  }


}






class Query[T<:Any] (values: Product ){
  def query: String = values.toString

  def columns: List[Column[Any]] = values.productIterator.toList.asInstanceOf[List[Column[Any]]]

  override def toString: String = values.toString



  def mapTo[T](resultSet: ResultSet)(implicit converter: Mapable.CaseMapable[T] ): Unit = {
    while (resultSet.next()){
      converter.mapTo(columns.map(column => column.retrieve(resultSet)))
    }
  }
}





object Query{

  def apply[T <: Any](values: Product): Query[T] = new Query(values)


}




object ColumnTest extends  App{

  case class Result(column_name: String, data_type: String)

  //def mapTo[R <: Product with Serializable](implicit rCT: TypeTag[R]): TypeTag[R] = rCT

  //implicit def typTagConverter(arg: Result):  TypeTag[Result] = mapTo[Result]

  class ResultTable extends Table[Result](name= "result") {
    def column_name = Column[String]("COLUMN_NAME")
    def data_type = Column[String](name = "DATA_TYPE")

    def * =  (column_name, data_type)
  }


  val table  = new ResultTable

}

///Map to shoudl return a function which implicitly maps to a collection of the case class