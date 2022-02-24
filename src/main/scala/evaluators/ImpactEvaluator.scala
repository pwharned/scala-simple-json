package evaluators

import database._
import json.Json.JsonMap
import json.Json.JsonProduct
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.reflect.runtime.universe._

object ImpactEvaluator {

  class Impact[T: TypeTag](prediction: String, table: String, protected_column: String, scoring_timestamp: String  = "scoring_timestamp",connection: AbstractDatabaseConnection[_]){




    class GroupedTable extends Table[T](name= table) {
      def prediction_column = Column[String](prediction).as("prediction")
      def protected_attribute_column = Column[String](name = protected_column).count("group")
      def minutes = Column[String](name = scoring_timestamp).aggregate("minutes", "minute")
      def hours = Column[String](name = scoring_timestamp).aggregate("hours", "hour")
      def maxDays = Column[String](name = scoring_timestamp).aggregate("days", "day")
      def days = Column[String](name = scoring_timestamp).aggregate("days", "day")

      def * =  (prediction_column, minutes, hours, days, protected_attribute_column, protected_attribute_column.unpack).filter(days, "=", table = maxDaysTable) .groupBy(protected_attribute_column.unpack, prediction_column, minutes, hours, days)
    }

    class MaxDaysTable extends Table[T](name = table) {
      def maxDays = Column[String](name = scoring_timestamp).aggregate("days", "day").aggregate("days", "max")

      def * = maxDays

    }

    class RatiosTable extends Table[T](name = "t1") {
      def prediction_column = Column[String](name= "prediction")
      def protected_attribute_column = Column[String](name = protected_column)
      def group = Column[String](name = "group").cast(datatype = "float")

      def minutes = Column[String](name = "minutes")

      def ratio = Column[String](name = "group").aggregateOver("ratios", "sum", protected_attribute_column, minutes )

      def ratios = group/ratio

      def * = (prediction_column, protected_attribute_column,group.as("group"), minutes, ratios)

    }

    class ResultsTable extends RatiosTable{
      override def  ratio = Column[String](name = "ratios")
      override def  ratios = Column[String](name = "ratios").aggregateOver("ratios", "sum", prediction_column, minutes )

      override def * = (prediction_column, protected_attribute_column,group.as("group"), minutes, ratio, ratios)


    }

    def maxDaysTable = new MaxDaysTable()


    def result = new ResultsTable()



  }

}


object impactTest extends App{


  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")


  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")

  implicit val connection = DatabaseConnection(configuraiton)

  case class Result(prediction: String, group:String)


  val result = new ImpactEvaluator.Impact[Result]("risk", "test_data2", "sex", scoring_timestamp = "timestamp",connection = connection)
  println(result.result.*.toString)

println(result.result.toString)


  // var res = Await.result(result.result.flatMap, 5.seconds).map(x => json.Json.JsonProduct(x).toString).mkString(",")



}