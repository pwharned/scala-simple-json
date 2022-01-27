package evaluators

import database._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.reflect.runtime.universe._

object ImpactEvaluator {

  class Impact[T: TypeTag](prediction: String, table: String, protected_column: String, connection: AbstractDatabaseConnection[_]){


    class ResultTable extends Table[T](name= table) {
      def prediction_column = Column[String](prediction).as("prediction")
      def protected_attribute_column = Column[String](name = protected_column).count("group")

      def * =  (prediction_column, protected_attribute_column).groupBy(protected_attribute_column, prediction_column)
    }

    def result = new ResultTable()



  }

}


object impactTest extends App{


  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")


  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

  implicit val connection = DatabaseConnection(configuraiton)

  case class Result(prediction: String, group:String)

  val result = new ImpactEvaluator.Impact[Result]("NAME", "SYSIBM.SYSCOLUMNS", "TYPENAME", connection)

  println(result.result.toString)
  println(result.result.*.columns.map(x =>x.alias))

  println(Await.result(result.result.flatMap, 5.seconds))
}