package database

import java.sql.ResultSet
import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


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

  implicit object LongQueryable extends Queryable[Long]{
    override def retrieve(columnName: String, resultSet: ResultSet):Long  = resultSet.getLong(columnName)
    override def retrieve(columnName: String, resultSet: Future[ResultSet]): Future[Long]  = resultSet.map(x =>x.getLong(columnName))
  }
  implicit object DoubleQueryable extends Queryable[Double]{
    override def retrieve(columnName: String, resultSet: ResultSet):Double  = resultSet.getDouble(columnName)
    override def retrieve(columnName: String, resultSet: Future[ResultSet]): Future[Double]  = resultSet.map(x =>x.getDouble(columnName))
  }
  implicit object FloatQueryable extends Queryable[Float]{
    override def retrieve(columnName: String, resultSet: ResultSet):Float  = resultSet.getFloat(columnName)
    override def retrieve(columnName: String, resultSet: Future[ResultSet]): Future[Float]  = resultSet.map(x =>x.getFloat(columnName))
  }
}

trait Mapable {
  val classLoaderMirror = runtimeMirror(getClass.getClassLoader)

  class CaseMapable[T:TypeTag]{


    val tpe = typeOf[T]
    val classSymbol = tpe.typeSymbol.asClass

    if (!(tpe <:< typeOf[Product] && classSymbol.isCaseClass)) {
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
