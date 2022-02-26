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
      def group = Column[Float](name = "group").cast(datatype = "float")

      def minutes = Column[Int](name = "minutes")
      def hours = Column[Int](name = "hours")
      def days = Column[Int](name = "days")

      def ratio = Column[Double](name = "group").aggregateOver("ratios", "sum", protected_attribute_column, minutes, hours, days )

      def ratios = group/ratio

      def * = (prediction_column, protected_attribute_column,group.as("group"), minutes, ratios, hours, days)

    }

    class ResultsTable extends RatiosTable {

      override  val tableName: String = "t2"

      override def ratio = Column[Double](name = "ratios").as("disparate_impact")

      def disparate_impact = ratio/Column[Double](name = "ratios").aggregateOver("disparate_impact", "sum", prediction_column, minutes, hours, days)-ratio

      override def * = (prediction_column, protected_attribute_column, group.as("group"), disparate_impact, minutes, hours, days)

    }

    def maxDaysTable = new MaxDaysTable()

    def result = new RatiosTable()+ new GroupedTable() + new ResultsTable()

  }

}


