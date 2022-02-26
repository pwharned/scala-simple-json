import TableTest.columnA.columnName
import database.Column
import database.Table
import database.GenericTable
import database.Query
object  TableTest extends App {

  /*
  Test that a table can be created from a sequence of columns
   */

  val columnA: Column[String] = Column("columnA")
  val columnB: Column[Int] = Column("columnB")
  val columnSeq: List[Column[_]] = List(columnA, columnB)
  case class Result(columnA: String, columnB: String)
  val table = new GenericTable[Result](name = "testTable", values = columnSeq)

  println(table)

  /*
  Test that a query can be built from a sequence of columns
   */
  val query = new Query(columnSeq).asInstanceOf[Query[Column[Any]]]

  println(query)


}
