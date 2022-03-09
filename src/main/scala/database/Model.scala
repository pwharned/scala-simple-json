package database


class Model(values: List[Column[Any]], target: String = "target", learn_rate: String = ".021", max_iter: String = "100000", limit: String = "1000" )(implicit tableName: String) extends Query[Column[Any]](values = values) {


  val features: List[String] = values.map(x => x.columnName)

  def coefficients(string: String = "M"): List[String] = (1 to values.length).map( x => string + x).toList

  val feature_names: Map[String, String] = coefficients().zip(features).toMap

  def equation: String = f"((${coefficients("B").zip(features).toMap.map( x=> f"(Z.${x._2}*A.${x._1})" ).mkString("+")})+A.INTERCEPT)"

  def difference: String = f"(${equation}-z.target)"

  def betas: String = f"${feature_names.map( x=> f"A.${x._1}-(SUM(G.DIFFERENCES*G.${x._2})/(SELECT COUNT FROM RATES))*(SELECT LEARN_RATE from RATES) AS ${x._1}").mkString(",\n")}"

  def error: String  = f"(Z.TARGET-${equation})"

  def mse: String  = f"AVG(g.differences*g.differences) AS MSE"

  def intercept: String =  f"A.C-(SUM(g.differences)/(SELECT COUNT from RATES))*(SELECT LEARN_RATE FROM RATES) AS C"

  def rates: String = f"RATES (LEARN_RATE, COUNT) AS (SELECT ${learn_rate} AS LEARN_RATE, (SELECT COUNT(*) FROM TRAINING) AS COUNT FROM SYSIBM.SYSDUMMY1)"

  def table: String = f"TABLE \n ( \n SELECT ${List("A.ITERATION", mse, betas, intercept).mkString(",\n")} \n FROM ${g}) t"

  def train: String = f"TRAINING AS (SELECT t.*, t.${target} as target, ROW_NUMBER() OVER() AS ROW FROM ${tableName}  AS T order by random() limit $limit  )"
  /*
  Ordering by random will result in a performance degradation for large tables so we need to consider various better approaches or constraining the table to windows.
   */

  def g: String = f"\nTABLE ( SELECT \n${difference} as differences,\n  ${features.map( x=> f"z.${x}").mkString(",\n")} \n, z.target  \n from training as z \n)  \nas g"

  def learning: String = f"LEARNING (ITERATION, ${coefficients("B").mkString(",")}, INTERCEPT, MSE, ${coefficients().mkString(",")}, C) AS (SELECT 1, ${(1 to ((features.length*2)+3)).map(x => "CAST(0.0 AS DOUBLE)").mkString(",")} FROM SYSIBM.SYSDUMMY1 ${learningEnd} "

  def learningEnd: String = s"\n UNION ALL SELECT A.ITERATION +1, ${feature_names.map(x =>f"A.${x._1}").mkString(",")}, A.C, T.MSE, ${feature_names.map(x =>f"T.${x._1}").mkString(",")}, T.C FROM LEARNING A, \n${table}\n WHERE A.ITERATION <= ${max_iter}) \n"

  def query: String = s"WITH \n ${List(train, rates, learning).mkString(", \n")} SELECT * FROM LEARNING \n WHERE ITERATION >1 ORDER BY MSE LIMIT 1 "

  override def toString: String = query




  //def columns: List[T] = new Column[Double]("iteration") :: (new Column[Double]("mse") :: coefficients("b").map( x=> new Column[Double](x))  )
  override def columns: List[Column[Any]] = new Column[Double]("iteration") :: (new Column[Double]("mse") :: coefficients("b").map( x=> new Column[Double](x))  )


}

