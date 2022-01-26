package database

trait DataType

object DataType {

  case class StringType(s: String) extends DataType
  case class IntegerType(i: Int) extends DataType
  case class DoubleType(i: Double) extends DataType

}

trait Typeable[T]{
  def serialize(t: T): DataType
}
object Typeable{
  implicit object StringTypeable extends Typeable[String]{
    def serialize(t: String) = DataType.StringType(t)
  }
  implicit object DoubleTypeable extends Typeable[Double]{
    def serialize(t: Double) = DataType.DoubleType(t)
  }
  implicit object IntJsonable extends Typeable[Int]{
    def serialize(t: Int) = DataType.IntegerType(t)
  }
}



