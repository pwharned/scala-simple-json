package database

class Model[+T<:Any] (values: List[Column[T]], target: String = "target", learn_rate: String = ".021", max_iter: String = "100000", limit: String = "1000" )(implicit tableName: String) extends Query(values = values) {


  val features: List[String] = values.map(x => x.columnName)

  def coefficients(string: String = "M"): List[String] = (1 to values.length).map( x => string + x).toList

  val feature_names: Map[String, String] = coefficients().zip(features).toMap

  def equation: String = f"(${feature_names.map( x=> f"(TRAIN.${x._2}*A.${x._1})" ).mkString("+")}+A.INTERCEPT)"

  def difference: String = f"(${equation}-TRAIN.${target})"

  def betas: String = f"${feature_names.map( x=> f"A.${x._1}-(SUM(${difference}*TRAIN.${x._2})/(SELECT COUNT FROM RATES))*(SELECT LEARN_RATE from RATES) AS ${x._1}").mkString(",\n")}"

  def error(target: String): String  = f"(TRAIN.${target}-${equation})"

  def mse: String  = f"AVG(${error(target)}*${error(target)}) AS MSE"

  def intercept: String =  f"A.C-(SUM(${difference})/(SELECT COUNT from RATES))*(SELECT LEARN_RATE FROM RATES) AS C"

  def rates: String = f"RATES (LEARN_RATE, COUNT) AS (SELECT ${learn_rate} AS LEARN_RATE, (SELECT COUNT(*) FROM TRAINING) AS COUNT FROM SYSIBM.SYSDUMMY1)"

  def table: String = f"TABLE \n ( \n SELECT ${List("A.ITERATION", mse, betas, intercept).mkString(",\n")} \n FROM TRAINING AS TRAIN) t"

  def train: String = f"TRAINING AS (SELECT t.*, ROW_NUMBER() OVER() AS ROW FROM ${tableName}  AS T order by random() limit $limit  )"
  /*
  Ordering by random will result in a performance degradation for large tables so we need to consider various better approaches or constraining the table to windows.
   */

  def learning: String = f"LEARNING (ITERATION, ${coefficients("B").mkString(",")}, INTERCEPT, MSE, ${coefficients().mkString(",")}, C) AS (SELECT 1, ${(1 to ((features.length*2)+3)).map(x => "CAST(0.0 AS DOUBLE)").mkString(",")} FROM SYSIBM.SYSDUMMY1 ${learningEnd} "

  def learningEnd: String = s"\n UNION ALL SELECT A.ITERATION +1, ${feature_names.map(x =>f"A.${x._1}").mkString(",")}, A.C, T.MSE, ${feature_names.map(x =>f"T.${x._1}").mkString(",")}, T.C FROM LEARNING A, \n${table}\n WHERE A.ITERATION <= ${max_iter}) \n"

  def query: String = s"WITH \n ${List(train, rates, learning).mkString(", \n")} SELECT * FROM LEARNING \n ORDER BY MSE LIMIT 10 "

  override def toString: String = query

  override def columns: List[Column[Any]] = coefficients("b").map( x=> new Column[Double](x)) :+ new Column[Int]("iteration") :+ new Column[Double]("mse")



}
