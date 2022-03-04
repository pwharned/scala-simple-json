import database.{Column, ConcreteDatabaseConfiguration, DatabaseConnection, GenericTable, Model, Query}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object ModelTest extends App {

    /*
    Test that a table can be created from a sequence of columns
     */

  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")

  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

    val columnA: Column[String] = Column("petal_width")
    val columnB: Column[Int] = Column("petal_length")
    val columnSeq: List[Column[_]] = List(columnA, columnB)
    case class Result(b1: Double, b2: Double, iteration: Int, mse: Double)
    val table = new GenericTable[Result](name = "samples_view", values = columnSeq)

  val model = table.*.queryToModel(learn_rate = ".021", max_iter = "1000", target = "target")

  val modelTable = table.asModel("target", ".021", "10000")
  implicit val connection = DatabaseConnection(configuraiton)

  print(modelTable.*)

  println(modelTable.*.columns)
  println(modelTable.*.columns.map(x=> x.alias))

  println(Await.result(modelTable.flatMap, 20.seconds))


    /*
    Test that a query can be built from a sequence of columns
     */
    //val query = new Query(columnSeq).asInstanceOf[Query[Column[Any]]]




}
