package database

trait Clause



trait GroupBy extends Clause{

  implicit def clause(columnName: String*): String => String = {
    val columns = columnName.mkString(" , ")
    implicit transformer: String  => {

      s" GROUP BY $columns"
  }}
}



object Mutatable {
  implicit def innerObj[T](o: Mutated[T]):T = o.obj

  def ::[T](o: T) = new Mutated(o)

  class Mutated[+T] private[Mutatable](val obj: T) extends Clause

  object GroupAble {
    implicit def innerObj[T](o: Grouped[T]):T = o.obj

    def ::[T](o: T) = new Grouped(o)

    final class Grouped[+T] private[GroupAble](override val obj: T) extends Mutated[T](obj = obj) with GroupBy


  }

}




