import Main.configuraiton
import database.{ConcreteDatabaseConfiguration, DatabaseConnection}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object BasicQueryTest extends App {
  val currentDirectory = new java.io.File(".").getCanonicalPath

  print(currentDirectory +   "/project/database.json")

  val configuraiton = ConcreteDatabaseConfiguration( currentDirectory +   "/project/database.json")
  implicit val connection = DatabaseConnection(configuraiton)


  val query =
    """
      WITH
       TRAINING AS (SELECT t.*, ROW_NUMBER() OVER() AS ROW FROM samples_view AS T ),
      RATES (LEARN_RATE, COUNT) AS (SELECT .021 AS LEARN_RATE, (SELECT COUNT(*) FROM TRAINING) AS COUNT FROM SYSIBM.SYSDUMMY1),
      LEARNING (ITERATION, B1,B2, INTERCEPT, MSE, M1,M2, C) AS (SELECT 1, CAST(0.0 AS DOUBLE),CAST(0.0 AS DOUBLE),CAST(0.0 AS DOUBLE),CAST(0.0 AS DOUBLE),CAST(0.0 AS DOUBLE),CAST(0.0 AS DOUBLE),CAST(0.0 AS DOUBLE) FROM SYSIBM.SYSDUMMY1
       UNION ALL SELECT A.ITERATION +1, A.M1,A.M2, A.C, T.MSE, T.M1,T.M2, T.C FROM LEARNING A,
      TABLE
       (
       SELECT A.ITERATION,
      AVG((TRAIN.target-((TRAIN.petal_width*A.M1)+(TRAIN.petal_length*A.M2)+A.INTERCEPT))*(TRAIN.target-((TRAIN.petal_width*A.M1)+(TRAIN.petal_length*A.M2)+A.INTERCEPT))) AS MSE,
      A.M1-(SUM((((TRAIN.petal_width*A.M1)+(TRAIN.petal_length*A.M2)+A.INTERCEPT)-TRAIN.target)*TRAIN.petal_width)/(SELECT COUNT FROM RATES))*(SELECT LEARN_RATE from RATES) AS M1,
      A.M2-(SUM((((TRAIN.petal_width*A.M1)+(TRAIN.petal_length*A.M2)+A.INTERCEPT)-TRAIN.target)*TRAIN.petal_length)/(SELECT COUNT FROM RATES))*(SELECT LEARN_RATE from RATES) AS M2,
      A.C-(SUM((((TRAIN.petal_width*A.M1)+(TRAIN.petal_length*A.M2)+A.INTERCEPT)-TRAIN.target))/(SELECT COUNT from RATES))*(SELECT LEARN_RATE FROM RATES) AS C
       FROM TRAINING AS TRAIN) t
       WHERE A.ITERATION <= 1000)
        SELECT * FROM LEARNING
       ORDER BY MSE LIMIT 10
      """


  val result = Await.result(connection.execute(query), 30.seconds)

  while(result.next()){
    println(result.getString(""))
  }


}
